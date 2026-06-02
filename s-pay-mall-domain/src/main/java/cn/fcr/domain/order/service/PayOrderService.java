package cn.fcr.domain.order.service;

import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.valobj.PayStatus;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayApiException;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
public class PayOrderService {

    private final AlipayClient alipayClient;
    private final String notifyUrl;
    private final String returnUrl;

    public PayOrderService(AlipayClient alipayClient, String notifyUrl, String returnUrl) {
        this.alipayClient = alipayClient;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
    }

    public String generatePayUrl(PayOrderEntity payOrder) {
        if (!payOrder.canPay() && payOrder.getStatus() != PayStatus.WAIT_PAY) {
            throw new IllegalStateException("当前状态不允许生成支付链接: " + payOrder.getStatus().getDescription());
        }

        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(notifyUrl);
            request.setReturnUrl(returnUrl);

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", payOrder.getOrderNo());
            bizContent.put("total_amount", payOrder.getTotalAmount().toString());
            bizContent.put("subject", "商城订单支付");
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
            request.setBizContent(bizContent.toString());

            String payUrl = alipayClient.pageExecute(request).getBody();

            if (payUrl != null && !payUrl.isBlank()) {
                payOrder.initPayUrl(payUrl);
                log.info("支付宝支付表单生成成功，orderNo={}", payOrder.getOrderNo());
            }

            return payUrl;
        } catch (AlipayApiException e) {
            log.error("支付宝支付表单生成失败，orderNo={}", payOrder.getOrderNo(), e);
            payOrder.markFailed();
            throw new RuntimeException("支付链接生成失败", e);
        }
    }

    public boolean verifyCallbackSign(Map<String, String> params, String alipayPublicKey) {
        try {
            com.alipay.api.internal.util.AlipaySignature alipaySignature =
                new com.alipay.api.internal.util.AlipaySignature();
            return alipaySignature.rsa256CheckContent(
                getSignCheckContentV1(params),
                params.get("sign"),
                alipayPublicKey,
                "UTF-8"
            );
        } catch (AlipayApiException e) {
            log.error("支付宝签名验证异常", e);
            return false;
        }
    }

    private String getSignCheckContentV1(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if ("sign".equals(entry.getKey()) || "sign_type".equals(entry.getKey())) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    public void processPayCallback(PayOrderEntity payOrder, Map<String, String> callbackParams) {
        payOrder.verifyCallbackSign(
            callbackParams,
            payOrder.getOrderNo(),
            payOrder.getTotalAmount()
        );

        String tradeNo = callbackParams.get("trade_no");
        payOrder.completePay(tradeNo);

        log.info("支付回调处理完成，orderNo={}，tradeNo={}", payOrder.getOrderNo(), tradeNo);
    }

    public PayOrderEntity buildFromParams(String orderNo, String userId, String productId,
                                          String productName, BigDecimal totalAmount) {
        return PayOrderEntity.create(orderNo, userId, productId, productName, totalAmount);
    }
}