package cn.fcr.domain.order.gateway;

import cn.fcr.domain.shared.model.entity.PayOrderEntity;

/**
 * @author 傅崇睿
 * @description 支付网关接口
 */
public interface IPaymentGateway {

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
}