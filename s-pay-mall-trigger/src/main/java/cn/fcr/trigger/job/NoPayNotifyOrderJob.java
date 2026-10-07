package cn.fcr.trigger.job;

import cn.fcr.domain.order.gateway.IAlipayQueryGateway;
import cn.fcr.application.OrderApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 支付回调补偿Job
 *
 * <p>【TD-8】多实例部署时通过 Redisson 分布式锁（{@code lock:job:no-pay-notify}）防重，
 * 拿不到锁的实例直接跳过本轮，避免重复补单/重复关单。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@Component()
public class NoPayNotifyOrderJob {

    /** 订单应用层服务 */
    @Resource
    private OrderApplicationService orderApplicationService;

    /** 支付宝交易查询网关 */
    @Resource
    private IAlipayQueryGateway alipayQueryGateway;

    /** Redisson 客户端（TD-8：Job 分布式锁） */
    @Resource
    private RedissonClient redissonClient;

    /** Job 分布式锁 key（新建 key，遵循 lock: 前缀规范，不影响既有 key 语义） */
    private static final String JOB_LOCK_KEY = "lock:job:no-pay-notify";

    /** 锁租约 60 秒：一轮补偿处理远小于该时长，租约兜底防实例宕机后死锁 */
    private static final long LOCK_LEASE_SECONDS = 60L;

    /**
     * 定时补偿未收到回调的支付订单
     *
     * <p>每30秒执行一次，两个职责：</p>
     * <ol>
     *   <li>【补单】查询等待支付超过5分钟且未收到回调的订单，经支付宝二次确认
     *   已支付的走履约路径（tradeNo 传 null，补单路径不取回支付宝交易号）；</li>
     *   <li>【兜底关单，TD-7】查询等待支付超过40分钟仍未关闭的订单（正常 30 分钟延时
     *   关单消息的漏网之鱼），走 handleTimeoutCloseOrder 既有路径：支付宝二次确认
     *   已支付转履约、未支付关单并恢复库存。状态机条件更新守卫保证幂等。</li>
     * </ol>
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void exec() {
        RLock lock = redissonClient.getLock(JOB_LOCK_KEY);
        try {
            // tryLock(waitTime=0)：不等待，拿不到说明其他实例正在执行本轮补偿，直接跳过
            if (!lock.tryLock(0, LOCK_LEASE_SECONDS, TimeUnit.SECONDS)) {
                log.debug("未获取到补偿Job分布式锁，本轮跳过: lockKey={}", JOB_LOCK_KEY);
                return;
            }
            try {
                compensateNoPayNotify();
                compensateStaleUnpaidOrders();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("补偿Job获取分布式锁被中断", e);
        } catch (Exception e) {
            log.error("检测未接收到或未正确处理的支付回调通知失败", e);
        }
    }

    /**
     * 补单补偿：等待支付超5分钟未收到回调的订单，支付宝侧已成功则触发履约
     */
    private void compensateNoPayNotify() {
        log.info("任务；检测未接收到或未正确处理的支付回调通知");
        List<String> orderIds = orderApplicationService.queryNoPayNotifyOrder();
        if (null == orderIds || orderIds.isEmpty()) return;

        for (String orderId : orderIds) {
            if (alipayQueryGateway.queryTradeSuccess(orderId)) {
                // 补单路径仅确认支付成功，未取回支付宝交易号，tradeNo 传 null
                orderApplicationService.changeOrderPaySuccess(orderId, null);
            }
        }
    }

    /**
     * 兜底关单补偿（TD-7）：等待支付超40分钟仍未关闭的订单，
     * 经 handleTimeoutCloseOrder 既有路径处理（支付宝二次确认 → 关单/转履约 + 库存恢复）。
     * 单条失败不影响其他订单补偿，异常上抛由外层统一捕获记日志。
     */
    private void compensateStaleUnpaidOrders() {
        List<String> staleOrderNos = orderApplicationService.queryStaleWaitPayOrders();
        if (null == staleOrderNos || staleOrderNos.isEmpty()) return;

        log.info("任务；兜底关单补偿，发现 {} 个超时未关单订单", staleOrderNos.size());
        for (String orderNo : staleOrderNos) {
            try {
                boolean handled = orderApplicationService.handleTimeoutCloseOrder(orderNo);
                log.info("兜底关单补偿处理完成: orderNo={}, handled={}", orderNo, handled);
            } catch (Exception e) {
                log.error("兜底关单补偿单条处理失败，继续处理后续订单: orderNo={}", orderNo, e);
            }
        }
    }

}
