package cn.fcr.types.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * MQ 事件抽象基类
 *
 * @author 傅崇睿
 */
@Data
public abstract class BaseEvent<T> {

    /**
     * 构建事件消息
     *
     * @param data 事件数据
     * @return 事件消息
     */
    public abstract EventMessage<T> buildEventMessage(T data);

    /**
     * 获取事件所属的 MQ Topic
     *
     * @return Topic 名称
     */
    public abstract String topic();

    /** 事件消息载体 */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EventMessage<T> {
        /** 消息唯一标识 */
        private String id;
        /** 消息时间戳 */
        private Date timestamp;
        /** 消息体数据 */
        private T data;
    }
}
