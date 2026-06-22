package cn.fcr.infrastructure.order.gateway;

import cn.fcr.domain.order.gateway.IOrderEventGateway;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @description 订单事件网关实现
 *
 * 【职责说明】
 * - 封装订单事件的消息发送逻辑
 * - 实现领域层定义的IOrderEventGateway接口
 * - 负责发送订单延时关闭消息、支付成功消息等
 *
 * 【核心功能】
 * 1. sendDelayCloseMessage(): 发送订单延时关闭消息到MQ
 * 2. sendPaySuccessMessage(): 发送支付成功消息到MQ
 *
 * 【依赖说明】
 * - RocketMQTemplate: RocketMQ消息发送模板
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
