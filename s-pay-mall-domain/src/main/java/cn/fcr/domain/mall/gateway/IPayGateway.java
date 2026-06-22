package cn.fcr.domain.mall.gateway;

import cn.fcr.domain.shared.model.entity.PayOrderEntity;

import java.util.Map;

/**
 * 支付网关接口（DDD 防腐层 ACL），抽象外部支付系统的业务操作，
 * 由 Infrastructure 层实现具体支付技术（如支付宝 SDK）。
 *
 * @author 傅崇睿
 */
public interface IPayGateway {

    /**
     * 生成支付链接 / 支付表单
     *
     * @param payOrder 支付订单实体
     * @return 支付页面 HTML 或 URL
     */
    String generatePayUrl(PayOrderEntity payOrder);

    /**
     * 验证支付回调签名
     *
     * @param params        回调参数
     * @param alipayPublicKey 支付宝公钥
     * @return true=验签通过，false=验签失败
     */
    boolean verifySignature(Map<String, String> params, String alipayPublicKey);
}
