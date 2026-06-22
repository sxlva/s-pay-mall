package cn.fcr.trigger.job;

import cn.fcr.domain.order.gateway.IAlipayQueryGateway;
import cn.fcr.trigger.application.OrderApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 支付回调补偿Job
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

    /**
     * 定时补偿未收到回调的支付订单
     *
     * <p>每30秒执行一次，查询未正确处理的支付回调通知，
     * 通过支付宝交易查询接口主动确认支付状态。</p>
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void exec() {
        try {
            log.info("任务；检测未接收到或未正确处理的支付回调通知");
            List<String> orderIds = orderApplicationService.queryNoPayNotifyOrder();
            if (null == orderIds || orderIds.isEmpty()) return;

            for (String orderId : orderIds) {
                if (alipayQueryGateway.queryTradeSuccess(orderId)) {
                    orderApplicationService.changeOrderPaySuccess(orderId);
                }
            }
        } catch (Exception e) {
            log.error("检测未接收到或未正确处理的支付回调通知失败", e);
        }
    }

}
