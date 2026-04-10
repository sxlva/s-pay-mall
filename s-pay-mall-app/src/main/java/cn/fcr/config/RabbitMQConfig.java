package cn.fcr.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description RabbitMQ 配置
 * @author 傅崇睿
 * @date 2025/8/30
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 支付成功事件消息配置 ====================
    public static final String PAY_SUCCESS_QUEUE = "pay_success";
    public static final String PAY_SUCCESS_EXCHANGE = "pay_success";
    public static final String PAY_SUCCESS_ROUTING_KEY = "pay_success";

    @Bean
    public Queue paySuccessQueue() {
        return new Queue(PAY_SUCCESS_QUEUE, true);
    }

    @Bean
    public TopicExchange paySuccessExchange() {
        return new TopicExchange(PAY_SUCCESS_EXCHANGE, true, false);
    }

    @Bean
    public Binding paySuccessBinding(Queue paySuccessQueue, TopicExchange paySuccessExchange) {
        return BindingBuilder.bind(paySuccessQueue).to(paySuccessExchange).with(PAY_SUCCESS_ROUTING_KEY);
    }

    // ==================== RabbitTemplate 配置（消息发送模板） ====================
    /**
     * RabbitTemplate 用于发送消息到 RabbitMQ
     * 配置：
     * - 启用消息发布确认（publisherConfirms）
     * - 启用强制投递（mandatory=true），确保消息被路由到队列
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 发送消息后确认回调（ACK）
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("消息发送成功，ID: " + correlationData.getId());
            } else {
                System.out.println("消息发送失败，ID: " + correlationData.getId() + ", 原因: " + cause);
            }
        });
        // 消息投递失败时回调（Return）
        rabbitTemplate.setReturnsCallback(returned -> {
            System.out.println("消息未被路由到队列，Exchange: " + returned.getExchange() + 
                             ", RoutingKey: " + returned.getRoutingKey() + 
                             ", ReplyText: " + returned.getReplyText());
        });
        return rabbitTemplate;
    }
}
