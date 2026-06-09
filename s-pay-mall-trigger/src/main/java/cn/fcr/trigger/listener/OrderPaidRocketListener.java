package cn.fcr.trigger.listener;

import cn.fcr.domain.order.adapter.event.PaySuccessMessageEvent;
import cn.fcr.domain.mall.gateway.IUserBindingGateway;
import cn.fcr.domain.mall.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.auth.gateway.IWeChatGateway;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RocketMQMessageListener(topic = "order_paid", consumerGroup = "s-pay-mall-order-paid-consumer")
public class OrderPaidRocketListener implements RocketMQListener<PaySuccessMessageEvent.PaySuccessMessage> {

    @Resource
    private IMallOrderService mallOrderService;

    @Resource
    private IMallOrderQueryGateway mallOrderQueryGateway;

    @Resource
    private IUserBindingGateway userBindingGateway;

    @Resource
    private IWeChatGateway weChatGateway;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void onMessage(PaySuccessMessageEvent.PaySuccessMessage message) {
        log.info("【RocketMQ 核心链路】收到支付成功消息，开始执行后续核心业务逻辑。订单号: {}, 交易号: {}",
                message.getOrderNo(), message.getTradeNo());

        try {
            mallOrderService.paySuccess(message.getOrderNo());
            log.info("订单状态更新成功，触发后续履约链路：开始发货、用户充值、发放会员权益、计算返利...");

            sendPaymentNotification(message.getOrderNo());

        } catch (Exception e) {
            log.error("处理支付成功消息异常，订单号: {}, 交易号: {}", message.getOrderNo(), message.getTradeNo(), e);
            throw new RuntimeException("处理支付成功消息失败", e);
        }
    }

    /**
     * 发送支付成功微信模板消息通知
     *
     * @param orderNo 订单号
     */
    private void sendPaymentNotification(String orderNo) {
        try {
            OrderEntity order = mallOrderQueryGateway.findByOrderNo(orderNo);
            if (order == null) {
                log.warn("发送支付通知失败：订单不存在，orderNo={}", orderNo);
                return;
            }

            String openid = userBindingGateway.getWeChatOpenIdByUserId(order.getUserId());
            if (openid == null) {
                log.info("用户未绑定微信，跳过支付通知推送，userId={}", order.getUserId());
                return;
            }

            String productName = order.getItems() != null && !order.getItems().isEmpty()
                    ? order.getItems().get(0).getProductName()
                    : "商品";
            String amount = order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0";
            String payTime = LocalDateTime.now().format(FORMATTER);

            weChatGateway.sendPaymentSuccessNotification(openid, productName, orderNo, amount, payTime);
            log.info("支付成功微信通知发送成功，orderNo={}, openid={}", orderNo, openid);

        } catch (Exception e) {
            log.error("发送支付成功微信通知异常，orderNo={}", orderNo, e);
        }
    }
}