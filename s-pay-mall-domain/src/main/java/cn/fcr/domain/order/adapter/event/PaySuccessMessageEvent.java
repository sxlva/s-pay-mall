package cn.fcr.domain.order.adapter.event;

import cn.fcr.types.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;

/**
 * 支付成功消息契约：定义消息结构（{@link PaySuccessMessage}）与 Topic 常量。
 * 消息体以裸 JSON 发送（不走 {@link BaseEvent#buildEventMessage} 信封），
 * 消费方为 OrderPaidRocketListener。
 *
 * @author 傅崇睿
 */
public class PaySuccessMessageEvent extends BaseEvent<PaySuccessMessageEvent.PaySuccessMessage> {

    /** 支付成功消息 Topic */
    public static final String TOPIC = "order_paid";

    @Override
    public EventMessage<PaySuccessMessage> buildEventMessage(PaySuccessMessage data) {
        return EventMessage.<PaySuccessMessage>builder()
                .id(RandomStringUtils.randomNumeric(11))
                .timestamp(new Date())
                .data(data)
                .build();
    }

    @Override
    public String topic() {
        return TOPIC;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaySuccessMessage{
        /** 用户ID */
        private String userId;
        /** 交易流水号 */
        private String tradeNo;
        /** 订单号 */
        private String orderNo;
    }
}
