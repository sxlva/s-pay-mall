package cn.fcr.domain.order.gateway;

/**
 * 支付宝交易查询网关接口（DDD 防腐层 ACL），抽象支付宝交易状态查询操作，
 * 领域层通过此接口查询支付状态，不直接依赖 Alipay SDK。
 *
 * @author 傅崇睿
 */
public interface IAlipayQueryGateway {

    /**
     * 查询支付宝交易状态
     *
     * @param orderNo 商户订单号
     * @return true=支付成功，false=未支付或查询失败
     */
    boolean queryTradeSuccess(String orderNo);
}
