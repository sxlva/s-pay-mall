package cn.fcr.infrastructure.mall.gateway;

import cn.fcr.domain.mall.gateway.IOrderPaymentGateway;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
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
    public void updatePayOrderInfo(PayOrderEntity payOrderEntity) {
        // 暂未实现：更新支付订单信息（需要补充数据库持久化逻辑）
        log.warn("updatePayOrderInfo 暂未实现，orderNo={}", payOrderEntity.getOrderNo());
    }

    @Override
    public void sendDelayCloseMessage(String orderNo) {
        log.info("发送订单延时关闭消息: orderNo={}", orderNo);
        try {
            // 【延时消息】delayLevel=5 对应 1 分钟，开发环境合理延时
            Message<String> message = MessageBuilder.withPayload(orderNo).build();
            rocketMQTemplate.syncSend("order-timeout-topic", message, 3000, 5);
            log.info("订单延时关闭消息发送成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("发送订单延时关闭消息失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
        }
    }
}
