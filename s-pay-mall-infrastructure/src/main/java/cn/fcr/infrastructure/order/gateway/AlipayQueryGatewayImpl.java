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
 * <p>
 * 【职责说明】
 * - 封装支付宝交易状态查询的 SDK 调用细节
 * - 实现领域层定义的 IAlipayQueryGateway 接口
 * - 将 Alipay SDK 的 "10000" 成功码语义转化为 boolean 返回值
 * </p>
 *
 * <p>
 * 【注意】此实现从 NoPayNotifyOrderJob 迁移而来，
 * 原 Job 中直接使用 AlipayClient 和 "10000".equals(code) 的业务码判断已封装于此。
 * </p>
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
