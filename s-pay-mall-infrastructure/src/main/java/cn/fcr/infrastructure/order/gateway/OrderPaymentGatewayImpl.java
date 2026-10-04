package cn.fcr.infrastructure.order.gateway;

import cn.fcr.domain.order.gateway.IOrderPaymentGateway;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.service.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 订单支付网关实现
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class OrderPaymentGatewayImpl implements IOrderPaymentGateway {

    @Resource
    private PayOrderService payOrderService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public String generatePayUrl(PayOrderEntity payOrderEntity) {
        return payOrderService.generatePayUrl(payOrderEntity);
    }

    @Override
    public void sendDelayCloseMessage(String orderNo) {
        log.info("发送订单延时关闭消息: orderNo={}", orderNo);
        try {
            // 【延时消息】delayLevel=9 对应 30 分钟（RocketMQ 官方级别：1s/5s/10s/30s/1m/2m/5m/10m/30m...），
            // 与下单到关单的业务支付超时窗口一致；B2 修复前为 level 5（1 分钟），
            // 用户付款时订单可能已被关闭，形成"钱已收、订单死"的死角
            Message<String> message = MessageBuilder.withPayload(orderNo).build();
            rocketMQTemplate.syncSend("order-timeout-topic", message, 3000, 9);
            log.info("订单延时关闭消息发送成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("发送订单延时关闭消息失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
        }
    }
}
