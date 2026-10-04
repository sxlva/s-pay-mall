package cn.fcr.infrastructure.order.gateway;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 支付宝交易查询网关单元测试（B1 回归保护）
 *
 * <p>锁定修复后的判定口径：code=10000 仅代表查询请求成功，
 * 交易支付成功必须同时满足 tradeStatus=TRADE_SUCCESS。
 *
 * @author 傅崇睿
 */
@ExtendWith(MockitoExtension.class)
class AlipayQueryGatewayImplTest {

    @Mock
    private AlipayClient alipayClient;

    @InjectMocks
    private AlipayQueryGatewayImpl alipayQueryGateway;

    /**
     * 构造查询响应（仅设置本测试关心的 code / tradeStatus 字段）
     *
     * @param code        支付宝网关业务码
     * @param tradeStatus 交易状态
     * @return 查询响应对象
     */
    private AlipayTradeQueryResponse buildResponse(String code, String tradeStatus) {
        AlipayTradeQueryResponse response = new AlipayTradeQueryResponse();
        response.setCode(code);
        response.setTradeStatus(tradeStatus);
        return response;
    }

    @Test
    void queryTradeSuccess_TradeSuccess_ReturnsTrue() throws Exception {
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class)))
                .thenReturn(buildResponse("10000", "TRADE_SUCCESS"));

        assertTrue(alipayQueryGateway.queryTradeSuccess("ORDER_001"));
    }

    @Test
    void queryTradeSuccess_WaitBuyerPay_ReturnsFalse() throws Exception {
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class)))
                .thenReturn(buildResponse("10000", "WAIT_BUYER_PAY"));

        assertFalse(alipayQueryGateway.queryTradeSuccess("ORDER_002"));
    }

    @Test
    void queryTradeSuccess_TradeClosed_ReturnsFalse() throws Exception {
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class)))
                .thenReturn(buildResponse("10000", "TRADE_CLOSED"));

        assertFalse(alipayQueryGateway.queryTradeSuccess("ORDER_003"));
    }

    @Test
    void queryTradeSuccess_QueryCodeNotSuccess_ReturnsFalse() throws Exception {
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class)))
                .thenReturn(buildResponse("40004", null));

        assertFalse(alipayQueryGateway.queryTradeSuccess("ORDER_004"));
    }

    @Test
    void queryTradeSuccess_CodeSuccessButTradeStatusMissing_ReturnsFalse() throws Exception {
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class)))
                .thenReturn(buildResponse("10000", null));

        assertFalse(alipayQueryGateway.queryTradeSuccess("ORDER_005"));
    }

    @Test
    void queryTradeSuccess_NullResponse_ReturnsFalse() throws Exception {
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class)))
                .thenReturn(null);

        assertFalse(alipayQueryGateway.queryTradeSuccess("ORDER_006"));
    }

    @Test
    void queryTradeSuccess_ClientThrowsException_ReturnsFalse() throws Exception {
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class)))
                .thenThrow(new RuntimeException("网络超时"));

        assertFalse(alipayQueryGateway.queryTradeSuccess("ORDER_007"));
    }
}
