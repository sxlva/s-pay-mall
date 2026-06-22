package cn.fcr.infrastructure.order.gateway;

import cn.fcr.domain.order.gateway.IOrderEventGateway;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
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
        rocketMQTemplate.syncSend("order-timeout-topic", orderNo, 3000);
    }

    @Override
    public void sendPaySuccessMessage(String orderNo) {
        log.info("发送支付成功消息: orderNo={}", orderNo);
        rocketMQTemplate.syncSend("pay-success-topic", orderNo);
    }
}
