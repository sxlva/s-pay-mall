package cn.fcr.trigger.listener;

import cn.fcr.domain.order.adapter.event.PaySuccessMessageEvent;
import cn.fcr.domain.mall.product.gateway.IIdempotentGateway;
import cn.fcr.application.OrderApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 订单支付成功 RocketMQ 消息监听器（P1-3：仅保留协议适配与消费语义，
 * 业务编排全部委托 OrderApplicationService）。
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = PaySuccessMessageEvent.TOPIC, consumerGroup = "s-pay-mall-order-paid-consumer",
        maxReconsumeTimes = 5)
public class OrderPaidRocketListener implements RocketMQListener<PaySuccessMessageEvent.PaySuccessMessage> {

    /** 订单应用服务 */
    @Resource
    private OrderApplicationService orderApplicationService;

    /** 幂等网关（P0-4：消费幂等守门） */
    @Resource
    private IIdempotentGateway idempotentGateway;

    /**
     * 消费支付成功消息：更新订单状态并发送微信模板消息通知。
     *
     * <p>【P0-4 消费幂等】以 orderNo 为幂等键（24h）：RocketMQ at-least-once 重投时
     * 直接跳过，不重复执行 paySuccess 与通知发送；处理异常时释放幂等键，
     * 交由 MQ 重试。成功则不释放——键 24h 自动过期，期间重投一律跳过。</p>
     *
     * @param message 支付成功消息体
     */
    @Override
    public void onMessage(PaySuccessMessageEvent.PaySuccessMessage message) {
        log.info("【RocketMQ 核心链路】收到支付成功消息，开始执行后续核心业务逻辑。订单号: {}, 交易号: {}",
                message.getOrderNo(), message.getTradeNo());

        // P0-4：消费幂等守门，重复消息直接 ACK 跳过（不抛异常，避免无意义重投）
        boolean acquired = idempotentGateway.tryAcquire(
                IIdempotentGateway.BUSINESS_TYPE_ORDER_PAID_NOTIFY, message.getOrderNo());
        if (!acquired) {
            log.info("【消费幂等】支付成功消息已消费过，跳过处理。订单号: {}", message.getOrderNo());
            return;
        }

        try {
            orderApplicationService.paySuccess(message.getOrderNo());
            log.info("订单状态更新成功: orderNo={}", message.getOrderNo());

            orderApplicationService.sendPaySuccessNotification(message.getOrderNo());

        } catch (Exception e) {
            log.error("处理支付成功消息异常，订单号: {}, 交易号: {}", message.getOrderNo(), message.getTradeNo(), e);
            // 异常时释放幂等键，允许 MQ 重试消费
            idempotentGateway.release(IIdempotentGateway.BUSINESS_TYPE_ORDER_PAID_NOTIFY, message.getOrderNo());
            throw new RuntimeException("处理支付成功消息失败", e);
        }
    }
}
