package cn.fcr.infrastructure.mall.gateway;

import cn.fcr.domain.mall.gateway.IPayGateway;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.infrastructure.config.AliPayConfigProperties;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayApiException;
import com.alipay.api.request.AlipayTradePagePayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 支付宝支付网关实现
 * <p>
 * 【DDD 防腐层 (ACL)】实现 mall 领域定义的 IPayGateway 接口，
 * 封装支付宝 AlipayClient SDK，将外部支付系统的技术细节隔离在基础设施层。
 */
@Slf4j
@Component
public class AlipayGatewayImpl implements IPayGateway {

    private final AlipayClient alipayClient;
    private final String notifyUrl;
    private final String returnUrl;

    public AlipayGatewayImpl(AlipayClient alipayClient, AliPayConfigProperties properties) {
        this.alipayClient = alipayClient;
        this.notifyUrl = properties.getNotify_url();
        this.returnUrl = properties.getReturn_url();
    }

    @Override
    public String generatePayUrl(PayOrderEntity payOrder) {
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

            log.info("支付宝支付表单生成成功，orderNo={}", payOrder.getOrderNo());
            return payUrl;

        } catch (AlipayApiException e) {
            log.error("支付宝支付表单生成失败，orderNo={}", payOrder.getOrderNo(), e);
            throw new RuntimeException("支付链接生成失败", e);
        }
    }

    @Override
    public boolean verifySignature(Map<String, String> params, String alipayPublicKey) {
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

    /**
     * 构建待签名字符串
     * 排除 sign 和 sign_type 字段，其余参数按 key 排序后拼接
     */
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
}
