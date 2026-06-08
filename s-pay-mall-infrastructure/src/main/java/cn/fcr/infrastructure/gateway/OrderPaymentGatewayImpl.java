package cn.fcr.infrastructure.gateway;

import cn.fcr.domain.mall.gateway.IOrderPaymentGateway;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.service.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author 傅崇睿
 * @description 订单支付网关实现
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
    public void updatePayOrderInfo(PayOrderEntity payOrderEntity) {
    }

    @Override
    public void sendDelayCloseMessage(String orderNo) {
        log.info("发送订单延时关闭消息: orderNo={}", orderNo);
        try {
            rocketMQTemplate.syncSend("order-close-topic", orderNo, 3000);
            log.info("订单延时关闭消息发送成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("发送订单延时关闭消息失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
        }
    }
}