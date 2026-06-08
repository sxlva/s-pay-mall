package cn.fcr.domain.mall.gateway;

import cn.fcr.domain.order.model.entity.PayOrderEntity;

/**
 * @author 傅崇睿
 * @description 订单支付网关接口
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