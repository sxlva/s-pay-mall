package cn.fcr.trigger.listener;

import cn.fcr.trigger.application.OrderApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * RocketMQ 超时关单消息消费者
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "order-timeout-topic", consumerGroup = "s-pay-mall-timeout-group")
public class OrderTimeoutCloseRocketListener implements RocketMQListener<String> {

    /** 订单应用层服务 */
    @Resource
    private OrderApplicationService orderApplicationService;

    /**
     * 消费超时关单延时消息
     *
     * <p>订阅 order-timeout-topic，接收延时消息进行超时关单处理。
     * 订单已支付或已取消时不会重复关闭。</p>
     *
     * @param orderNo 订单号
     */
    @Override
    public void onMessage(String orderNo) {
        log.info("接收到超时关单消息: orderNo={}", orderNo);

        try {
            boolean success = orderApplicationService.handleTimeoutCloseOrder(orderNo);
            if (success) {
                log.info("超时关单成功: orderNo={}", orderNo);
            } else {
                log.info("超时关单未执行（订单状态已变更或不存在）: orderNo={}", orderNo);
            }

        } catch (Exception e) {
            log.error("超时关单处理失败: orderNo={}", orderNo, e);
            throw new RuntimeException("超时关单处理失败: orderNo=" + orderNo, e);
        }
    }
}
