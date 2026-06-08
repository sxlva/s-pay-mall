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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
            String sign = params.get("sign");
            String signType = params.get("sign_type");
            
            log.info("【验签调试】收到的参数数量: {}, sign_type: {}, sign长度: {}", 
                    params.size(), signType, sign != null ? sign.length() : 0);
            log.info("【验签调试】sign值前50字符: {}", sign != null && sign.length() > 50 ? sign.substring(0, 50) : sign);
            
            String signCheckContent = getSignCheckContentV1(params);
            log.info("【验签调试】待验签字符串长度: {}, 前200字符: {}", 
                    signCheckContent.length(), 
                    signCheckContent.length() > 200 ? signCheckContent.substring(0, 200) : signCheckContent);
            
            com.alipay.api.internal.util.AlipaySignature alipaySignature =
                new com.alipay.api.internal.util.AlipaySignature();
            boolean result = alipaySignature.rsa256CheckContent(
                signCheckContent,
                sign,
                alipayPublicKey,
                "UTF-8"
            );
            
            log.info("【验签调试】验签结果: {}", result);
            return result;
        } catch (AlipayApiException e) {
            log.error("支付宝签名验证异常: {}", e.getMessage(), e);
            return false;
        }
    }

    private String getSignCheckContentV1(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if ("sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            String value = params.get(key);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key).append("=").append(value);
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