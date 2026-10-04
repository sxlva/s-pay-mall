package cn.fcr.domain.order.gateway;

import cn.fcr.domain.order.model.entity.PayOrderEntity;

/**
 * 订单支付网关接口，定义支付链接生成和延时关闭消息发送的抽象。
 *
 * <p>【落库口径】唤起支付宝仅改变内存中 PayOrderEntity 的状态（WAIT_PAY → PAYING），
 * pay_order 表保持 WAIT_PAY 落库直至支付回调/超时关单；超时关单与补单查询均以 WAIT_PAY 为准
 * （设计文档 module-order-pay.md §四/§五），因此本网关不提供"更新支付订单信息"类持久化方法。</p>
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
     * 发送订单延时关闭消息
     *
     * @param orderNo 订单号
     */
    void sendDelayCloseMessage(String orderNo);
}