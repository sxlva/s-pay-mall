package cn.fcr.trigger.listener;

import cn.fcr.domain.order.adapter.event.PaySuccessMessageEvent;
import cn.fcr.types.event.BaseEvent;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @description 支付成功消息消费者
 * @author 傅崇睿
 * @date 2025/8/30
 * 特性：
 * - 自动反序列化 BaseEvent.EventMessage 强类型
 * - 支付成功后触发通知和发货流程
 * - 异常自动重试（由 RabbitMQ 配置管理）
 */
@Slf4j
@Component
@RabbitListener(queues = "pay_success")
public class OrderPaySuccessConsumer {

    /**
     * 支付成功消息处理
     * 约束：
     * - 消息必须遵循 BaseEvent.EventMessage<PaySuccessMessage> 格式
     * - 异常会自动重试，最多 3 次（application-dev.yml 配置）
     * - 处理幂等性由业务层保证（通过订单ID）
     */
    @RabbitHandler
    public void onMessage(String message) {
        try {
            log.info("[支付成功队列] 接收到消息：{}", message);
            
            // 1. 消息解析（强类型，防止格式错误）
            BaseEvent.EventMessage<PaySuccessMessageEvent.PaySuccessMessage> eventMessage = 
                JSON.parseObject(message, 
                    new TypeReference<BaseEvent.EventMessage<PaySuccessMessageEvent.PaySuccessMessage>>() {});
            
            PaySuccessMessageEvent.PaySuccessMessage payData = eventMessage.getData();
            String orderId = eventMessage.getId();
            String userId = payData.getUserId();
            String tradeNo = payData.getTradeNo();
            
            log.info("[支付成功队列] 开始处理支付成功事件 - 订单ID: {}, 用户ID: {}, 交易号: {}", 
                    orderId, userId, tradeNo);
            
            // 2. 更新订单状态（幂等性保证由订单服务负责）
            // TODO: 调用订单服务更新订单为支付成功状态
            
            // 3. 发送用户通知（微信/短信）
            log.info("[支付成功队列] 推送用户通知 - 用户ID: {}, 交易号: {}", userId, tradeNo);
            // TODO: 调用通知服务发送订单支付成功通知
            
            // 4. 触发发货流程（异步，可发送新消息到发货队列）
            log.info("[支付成功队列] 触发发货逻辑 - 订单ID: {}", orderId);
            // TODO: 发送发货消息到 order_deliver 队列
            
            log.info("[支付成功队列] 支付成功事件处理完毕 - 订单ID: {}", orderId);
            
        } catch (Exception e) {
            log.error("[支付成功队列] 处理消息异常，消息内容: {}", message, e);
            // 异常会被 Spring AMQP 捕获，根据 application-dev.yml 配置自动重试
            // 重试 3 次后仍失败，消息进入死信队列（如配置）或被丢弃
            throw new RuntimeException("支付成功消息处理失败，触发重试机制", e);
        }
    }
}
