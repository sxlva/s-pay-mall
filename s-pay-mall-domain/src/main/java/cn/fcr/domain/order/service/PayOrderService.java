package cn.fcr.domain.order.service;

import cn.fcr.domain.mall.gateway.IPayGateway;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.domain.shared.model.vo.PayStatus;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PayOrderService {

    private final IPayGateway payGateway;

    public PayOrderService(IPayGateway payGateway) {
        this.payGateway = payGateway;
    }

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

    public boolean verifyCallbackSign(Map<String, String> params, String alipayPublicKey) {
        return payGateway.verifySignature(params, alipayPublicKey);
    }
}