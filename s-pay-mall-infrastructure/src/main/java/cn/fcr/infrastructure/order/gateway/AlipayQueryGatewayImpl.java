package cn.fcr.infrastructure.order.gateway;

import cn.fcr.domain.order.gateway.IAlipayQueryGateway;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝交易查询网关实现
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class AlipayQueryGatewayImpl implements IAlipayQueryGateway {

    private final AlipayClient alipayClient;

    public AlipayQueryGatewayImpl(AlipayClient alipayClient) {
        this.alipayClient = alipayClient;
    }

    @Override
    public boolean queryTradeSuccess(String orderNo) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel bizModel = new AlipayTradeQueryModel();
            bizModel.setOutTradeNo(orderNo);
            request.setBizModel(bizModel);

            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response == null) {
                log.warn("【支付宝查询】响应为空，orderNo={}", orderNo);
                return false;
            }

            String code = response.getCode();
            // 支付宝业务码：10000 仅表示"查询请求本身成功"，不代表交易支付成功；
            // 交易是否成功必须以 tradeStatus 为准（TRADE_SUCCESS），
            // 否则 WAIT_BUYER_PAY（待付款）/TRADE_CLOSED（已关闭）会被误判为支付成功
            if (!"10000".equals(code)) {
                log.info("【支付宝查询】查询请求未成功，orderNo={}, code={}, msg={}",
                        orderNo, code, response.getMsg());
                return false;
            }

            String tradeStatus = response.getTradeStatus();
            boolean tradeSuccess = "TRADE_SUCCESS".equals(tradeStatus);
            if (tradeSuccess) {
                log.info("【支付宝查询】交易支付成功，orderNo={}, tradeStatus={}", orderNo, tradeStatus);
            } else {
                log.info("【支付宝查询】交易未支付成功，orderNo={}, tradeStatus={}", orderNo, tradeStatus);
            }
            return tradeSuccess;
        } catch (Exception e) {
            log.error("【支付宝查询】查询交易状态异常，orderNo={}", orderNo, e);
            return false;
        }
    }
}
