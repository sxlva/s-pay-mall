package cn.fcr.infrastructure.order.gateway;

import cn.fcr.domain.order.gateway.IOrderEventGateway;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 订单事件网关实现（RocketMQ消息发送）
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class OrderEventGatewayImpl implements IOrderEventGateway {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void sendDelayCloseMessage(String orderNo) {
        log.info("发送订单延时关闭消息: orderNo={}", orderNo);
        // 【延时消息】delayLevel=5 对应 1 分钟，开发环境合理延时
        Message<String> message = MessageBuilder.withPayload(orderNo).build();
        rocketMQTemplate.syncSend("order-timeout-topic", message, 3000, 5);
    }

    @Override
    public void sendPaySuccessMessage(String orderNo) {
        log.info("发送支付成功消息: orderNo={}", orderNo);
        rocketMQTemplate.syncSend("pay-success-topic", orderNo);
    }
}
