package cn.fcr.domain.order.adapter.event;

/**
 * 订单事件发布接口（DDD 解耦），由 Infrastructure 层实现 RocketMQ 等技术细节，
 * 领域层仅依赖此抽象。
 *
 * @author 傅崇睿
 */
public interface IOrderEventPublisher {

    /**
     * 发布支付成功事件
     *
     * @param tradeNo 第三方交易号
     * @param orderNo 业务订单号
     */
    void publishPaySuccess(String tradeNo, String orderNo);
}
