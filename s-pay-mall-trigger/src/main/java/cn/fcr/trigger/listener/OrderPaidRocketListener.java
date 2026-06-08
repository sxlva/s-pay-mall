package cn.fcr.trigger.listener;

import cn.fcr.domain.order.adapter.event.PaySuccessMessageEvent;
import cn.fcr.domain.mall.service.IMallOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@RocketMQMessageListener(topic = "order_paid", consumerGroup = "s-pay-mall-order-paid-consumer")
public class OrderPaidRocketListener implements RocketMQListener<PaySuccessMessageEvent.PaySuccessMessage> {

    @Resource
    private IMallOrderService mallOrderService;

    @Override
    public void onMessage(PaySuccessMessageEvent.PaySuccessMessage message) {
        log.info("【RocketMQ 核心链路】收到支付成功消息，开始执行后续核心业务逻辑。订单号: {}, 交易号: {}",
                message.getOrderNo(), message.getTradeNo());

        try {
            mallOrderService.paySuccess(message.getOrderNo());
            log.info("订单状态更新成功，触发后续履约链路：开始发货、用户充值、发放会员权益、计算返利...");

        } catch (Exception e) {
            log.error("处理支付成功消息异常，订单号: {}, 交易号: {}", message.getOrderNo(), message.getTradeNo(), e);
            throw new RuntimeException("处理支付成功消息失败", e);
        }
    }
}