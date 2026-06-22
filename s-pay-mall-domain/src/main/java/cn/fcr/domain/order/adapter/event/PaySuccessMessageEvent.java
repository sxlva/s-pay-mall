package cn.fcr.domain.order.adapter.event;

import cn.fcr.types.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;

/**
 * 支付成功消息事件，定义支付成功消息的结构和消息主题 topic=pay_success。
 * 实例化由 Infrastructure 层实现。
 *
 * @author 傅崇睿
 */
public class PaySuccessMessageEvent extends BaseEvent<PaySuccessMessageEvent.PaySuccessMessage> {
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
        return "pay_success";
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
