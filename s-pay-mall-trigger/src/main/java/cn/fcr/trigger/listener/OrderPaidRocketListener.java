package cn.fcr.trigger.listener;

import cn.fcr.domain.order.adapter.event.PaySuccessMessageEvent;
import cn.fcr.domain.mall.product.gateway.IIdempotentGateway;
import cn.fcr.domain.mall.user.gateway.IUserBindingGateway;
import cn.fcr.domain.order.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.auth.login.gateway.IWeChatGateway;
import cn.fcr.application.OrderApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 订单支付成功 RocketMQ 消息监听器，触发后续履约与微信通知。
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "order_paid", consumerGroup = "s-pay-mall-order-paid-consumer",
        maxReconsumeTimes = 5)
public class OrderPaidRocketListener implements RocketMQListener<PaySuccessMessageEvent.PaySuccessMessage> {

    /** 订单应用服务 */
    @Resource
    private OrderApplicationService orderApplicationService;

    /** 订单查询网关 */
    @Resource
    private IMallOrderQueryGateway mallOrderQueryGateway;

    /** 用户绑定网关 */
    @Resource
    private IUserBindingGateway userBindingGateway;

    /** 微信网关 */
    @Resource
    private IWeChatGateway weChatGateway;

    /** 幂等网关（P0-4：消费幂等守门） */
    @Resource
    private IIdempotentGateway idempotentGateway;

    /** 支付时间格式化器 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 消费支付成功消息：更新订单状态并发送微信模板消息通知。
     *
     * <p>【P0-4 消费幂等】以 orderNo 为幂等键（24h）：RocketMQ at-least-once 重投时
     * 直接跳过，不重复执行 paySuccess 与通知发送；处理异常时释放幂等键，
     * 交由 MQ 重试。成功则不释放——键 24h 自动过期，期间重投一律跳过。</p>
     *
     * @param message 支付成功消息体
     */
    @Override
    public void onMessage(PaySuccessMessageEvent.PaySuccessMessage message) {
        log.info("【RocketMQ 核心链路】收到支付成功消息，开始执行后续核心业务逻辑。订单号: {}, 交易号: {}",
                message.getOrderNo(), message.getTradeNo());

        // P0-4：消费幂等守门，重复消息直接 ACK 跳过（不抛异常，避免无意义重投）
        boolean acquired = idempotentGateway.tryAcquire(
                IIdempotentGateway.BUSINESS_TYPE_ORDER_PAID_NOTIFY, message.getOrderNo());
        if (!acquired) {
            log.info("【消费幂等】支付成功消息已消费过，跳过处理。订单号: {}", message.getOrderNo());
            return;
        }

        try {
            orderApplicationService.paySuccess(message.getOrderNo());
            log.info("订单状态更新成功: orderNo={}", message.getOrderNo());

            sendPaymentNotification(message.getOrderNo());

        } catch (Exception e) {
            log.error("处理支付成功消息异常，订单号: {}, 交易号: {}", message.getOrderNo(), message.getTradeNo(), e);
            // 异常时释放幂等键，允许 MQ 重试消费
            idempotentGateway.release(IIdempotentGateway.BUSINESS_TYPE_ORDER_PAID_NOTIFY, message.getOrderNo());
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