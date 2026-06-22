package cn.fcr.infrastructure.order.event;

import cn.fcr.domain.order.adapter.event.IOrderEventPublisher;
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
            rocketMQTemplate.convertAndSend("order_paid", new PaySuccessMessage(tradeNo, orderNo));
            log.info("已发送支付成功消息到 RocketMQ: tradeNo={}, orderNo={}", tradeNo, orderNo);
        } catch (Exception e) {
            log.error("发送支付成功消息失败，但不影响订单状态更新: tradeNo={}, orderNo={}, error={}",
                    tradeNo, orderNo, e.getMessage());
        }
    }

    /**
     * 支付成功消息体
     * 仅限内部使用，不对 Domain 层暴露 MQ 消息结构
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class PaySuccessMessage {
        private String tradeNo;
        private String orderNo;
    }
}
