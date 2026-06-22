package cn.fcr.infrastructure.order.gateway;

import cn.fcr.domain.order.gateway.IAlipayQueryGateway;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 支付宝交易查询网关实现
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class AlipayQueryGatewayImpl implements IAlipayQueryGateway {

    @Resource
    private AlipayClient alipayClient;

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
            // 支付宝业务码：10000 表示请求成功
            if ("10000".equals(code)) {
                log.info("【支付宝查询】支付成功，orderNo={}", orderNo);
                return true;
            } else {
                log.info("【支付宝查询】支付未成功，orderNo={}, code={}, msg={}",
                        orderNo, code, response.getMsg());
                return false;
            }
        } catch (Exception e) {
            log.error("【支付宝查询】查询交易状态异常，orderNo={}", orderNo, e);
            return false;
        }
    }
}
