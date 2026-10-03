package cn.fcr.domain.order.service;

import cn.fcr.domain.order.gateway.IPayGateway;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.vo.PayStatus;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付订单领域服务，负责支付单状态流转与支付回调验签。
 *
 * <p>职责定死（M2-2）：支付单状态流转（生成支付链接 WAIT_PAY→PAYING、
 * 生成失败标记 FAILED）只允许经本服务入口；持久化由 gateway 承担，
 * 本服务不直接访问仓储。验签仅做参数合法性校验，不流转状态。</p>
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