package cn.fcr.domain.order.adapter.event;

/**
 * 订单事件发布接口
 * <p>
 * 【DDD 解耦】定义在 Domain 层的接口，由 Infrastructure 层实现具体的技术细节
 * （如 RocketMQ、Spring Event 等），领域层仅依赖此抽象。
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
