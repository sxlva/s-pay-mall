package cn.fcr.domain.order.service;

import cn.fcr.domain.mall.gateway.IPayGateway;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.domain.shared.model.vo.PayStatus;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付订单领域服务，负责生成支付链接和验证支付回调签名。
 *
 * @author 傅崇睿
 */
@Slf4j
public class PayOrderService {

    private final IPayGateway payGateway;

    public PayOrderService(IPayGateway payGateway) {
        this.payGateway = payGateway;
    }

    /**
     * 生成支付链接并初始化支付订单的支付URL
     *
     * @param payOrder 支付订单实体
     * @return 支付链接URL
     * @throws IllegalStateException 当前订单状态不允许支付
     * @throws RuntimeException 支付链接生成失败
     */
    public String generatePayUrl(PayOrderEntity payOrder) {
        if (!payOrder.canPay() && payOrder.getStatus() != PayStatus.WAIT_PAY) {
            throw new IllegalStateException("当前状态不允许生成支付链接: " + payOrder.getStatus().getDescription());
        }

        try {
            String payUrl = payGateway.generatePayUrl(payOrder);

            if (payUrl != null && !payUrl.isBlank()) {
                payOrder.initPayUrl(payUrl);
                log.info("支付表单生成成功，orderNo=" + payOrder.getOrderNo());
            }

            return payUrl;
        } catch (Exception e) {
            log.error("支付表单生成失败，orderNo=" + payOrder.getOrderNo());
            payOrder.markFailed();
            throw new RuntimeException("支付链接生成失败", e);
        }
    }

    /**
     * 验证支付回调签名
     *
     * @param params          回调参数
     * @param alipayPublicKey 支付宝公钥
     * @return 签名是否有效
     */
    public boolean verifyCallbackSign(Map<String, String> params, String alipayPublicKey) {
        return payGateway.verifySignature(params, alipayPublicKey);
    }
}