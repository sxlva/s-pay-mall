package cn.fcr.infrastructure.order.event;

import cn.fcr.domain.order.adapter.event.IOrderEventPublisher;
import cn.fcr.domain.order.adapter.event.PaySuccessMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * RocketMQ订单事件发布器
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class RocketMqOrderEventPublisher implements IOrderEventPublisher {

    private final RocketMQTemplate rocketMQTemplate;

    public RocketMqOrderEventPublisher(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public void publishPaySuccess(String tradeNo, String orderNo) {
        try {
            // 消息契约统一为 PaySuccessMessageEvent.PaySuccessMessage（topic=order_paid）：
            // userId 由消费方按 orderNo 反查，发布侧不携带
            PaySuccessMessageEvent.PaySuccessMessage message = PaySuccessMessageEvent.PaySuccessMessage.builder()
                    .tradeNo(tradeNo)
                    .orderNo(orderNo)
                    .build();
            // P1-4：显式指定 3000ms 发送超时（convertAndSend 无超时重载，使用 syncSend + Message）
            rocketMQTemplate.syncSend(PaySuccessMessageEvent.TOPIC,
                    org.springframework.messaging.support.MessageBuilder.withPayload(message).build(), 3000L);
            log.info("已发送支付成功消息到 RocketMQ: tradeNo={}, orderNo={}", tradeNo, orderNo);
        } catch (Exception e) {
            log.error("发送支付成功消息失败，但不影响订单状态更新: tradeNo={}, orderNo={}, error={}",
                    tradeNo, orderNo, e.getMessage());
        }
    }
}
