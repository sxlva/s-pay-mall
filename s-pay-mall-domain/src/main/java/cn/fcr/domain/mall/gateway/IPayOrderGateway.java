package cn.fcr.domain.mall.gateway;

import cn.fcr.domain.shared.model.vo.PayStatus;

/**
 * 支付订单网关接口，提供支付订单状态更新的能力。
 *
 * @author 傅崇睿
 */
public interface IPayOrderGateway {

    /**
     * 更新支付订单状态为已支付
     *
     * @param orderNo 订单号
     * @return 是否更新成功
     */
    boolean updatePayStatusToPaid(String orderNo);

    /**
     * 更新支付订单状态为交易完成
     *
     * @param orderNo 订单号
     * @return 是否更新成功
     */
    boolean updatePayStatusToTradeDone(String orderNo);

    /**
     * 关闭支付订单
     *
     * @param orderNo 订单号
     * @return 是否关闭成功
     */
    boolean closePayOrder(String orderNo);

    /**
     * 获取支付订单当前状态
     *
     * @param orderNo 订单号
     * @return 支付状态，若不存在返回 null
     */
    PayStatus getPayStatus(String orderNo);
}