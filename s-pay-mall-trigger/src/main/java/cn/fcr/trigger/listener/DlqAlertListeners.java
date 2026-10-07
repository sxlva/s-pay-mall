package cn.fcr.trigger.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列（DLQ）告警监听器（TD-2 最简落地）
 *
 * <p>消费端重试耗尽（maxReconsumeTimes=5）的消息由 broker 转入 {@code %DLQ%{consumerGroup}}，
 * 本组监听器订阅对应 DLQ 主题，收到死信即以 ERROR 级日志高调告警，
 * 消息体完整落日志作为人工重放素材（RocketMQ 控制台"消息查询→重新发送"即可完成重放）。
 * 消费成功返回（不回抛），保证同一条死信只告警一次、不再投递。</p>
 *
 * <p>说明：{@code s-pay-mall-stock-change-consumer} 组无生产者（TD-9 考证），
 * 其 DLQ 永不会写入消息，故不为其配置告警监听器。</p>
 *
 * @author 傅崇睿
 */
public class DlqAlertListeners {

    /**
     * order_paid 消费组的死信告警
     */
    @Slf4j
    @Component
    @RocketMQMessageListener(topic = "%DLQ%s-pay-mall-order-paid-consumer",
            consumerGroup = "s-pay-mall-dlq-alert-order-paid")
    public static class OrderPaidDlqAlertListener implements RocketMQListener<String> {
        @Override
        public void onMessage(String message) {
            log.error("【DLQ告警】order_paid 消息进入死信队列（重试5次耗尽），需人工核实并重放: body={}", message);
        }
    }

    /**
     * 超时关单消费组的死信告警
     */
    @Slf4j
    @Component
    @RocketMQMessageListener(topic = "%DLQ%s-pay-mall-timeout-group",
            consumerGroup = "s-pay-mall-dlq-alert-timeout-close")
    public static class TimeoutCloseDlqAlertListener implements RocketMQListener<String> {
        @Override
        public void onMessage(String message) {
            log.error("【DLQ告警】order-timeout-topic 消息进入死信队列（重试5次耗尽），需人工核实并重放: body={}", message);
        }
    }
}
