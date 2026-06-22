package cn.fcr.domain.order.gateway;

/**
 * 订单事件网关接口，定义订单延时关闭和支付成功消息的发送抽象
 *
 * @author 傅崇睿
 */
public interface IOrderEventGateway {

    /**
     * 发送订单延时关闭消息
     *
     * @param orderNo 订单号
     */
    void sendDelayCloseMessage(String orderNo);

    /**
     * 发送支付成功消息
     *
     * @param orderNo 订单号
     */
    void sendPaySuccessMessage(String orderNo);
}