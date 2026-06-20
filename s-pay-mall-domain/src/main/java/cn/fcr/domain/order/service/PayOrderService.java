package cn.fcr.domain.order.service;

import cn.fcr.domain.mall.gateway.IPayGateway;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.domain.shared.model.vo.PayStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.logging.Logger;

public class PayOrderService {

    private final Logger logger = Logger.getLogger(PayOrderService.class.getName());

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
                logger.info("支付表单生成成功，orderNo=" + payOrder.getOrderNo());
            }

            return payUrl;
        } catch (Exception e) {
            logger.severe("支付表单生成失败，orderNo=" + payOrder.getOrderNo());
            payOrder.markFailed();
            throw new RuntimeException("支付链接生成失败", e);
        }
    }

    public boolean verifyCallbackSign(Map<String, String> params, String alipayPublicKey) {
        return payGateway.verifySignature(params, alipayPublicKey);
    }

    public void processPayCallback(PayOrderEntity payOrder, Map<String, String> callbackParams) {
        payOrder.verifyCallbackSign(
            callbackParams,
            payOrder.getOrderNo(),
            payOrder.getTotalAmount()
        );

        String tradeNo = callbackParams.get("trade_no");
        payOrder.completePay(tradeNo);

        logger.info("支付回调处理完成，orderNo=" + payOrder.getOrderNo() + "，tradeNo=" + tradeNo);
    }

    public PayOrderEntity buildFromParams(String orderNo, String userId, String productId,
                                          String productName, BigDecimal totalAmount) {
        return PayOrderEntity.create(orderNo, userId, productId, productName, totalAmount);
    }
}