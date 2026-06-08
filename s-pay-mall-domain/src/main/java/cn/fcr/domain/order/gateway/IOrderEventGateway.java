package cn.fcr.domain.order.gateway;

/**
 * @author 傅崇睿
 * @description 订单事件网关接口
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