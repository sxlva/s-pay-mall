package cn.fcr.domain.mall.gateway;

import cn.fcr.domain.shared.model.entity.PayOrderEntity;

/**
 * 订单支付网关接口，定义支付链接生成、支付信息更新和延时关闭消息发送的抽象。
 *
 * @author 傅崇睿
 */
public interface IOrderPaymentGateway {

    /**
     * 生成支付 URL
     *
     * @param payOrderEntity 支付订单实体
     * @return 支付 URL
     */
    String generatePayUrl(PayOrderEntity payOrderEntity);

    /**
     * 更新支付订单信息
     *
     * @param payOrderEntity 支付订单实体
     */
    void updatePayOrderInfo(PayOrderEntity payOrderEntity);

    /**
     * 发送订单延时关闭消息
     *
     * @param orderNo 订单号
     */
    void sendDelayCloseMessage(String orderNo);
}