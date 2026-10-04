package cn.fcr.application;

import cn.fcr.domain.auth.login.gateway.IWeChatGateway;
import cn.fcr.domain.mall.cart.service.IMallCartService;
import cn.fcr.domain.mall.product.gateway.IIdempotentGateway;
import cn.fcr.domain.mall.user.gateway.IUserBindingGateway;
import cn.fcr.domain.order.adapter.event.IOrderEventPublisher;
import cn.fcr.domain.order.gateway.IAlipayQueryGateway;
import cn.fcr.domain.order.gateway.IOrderPaymentGateway;
import cn.fcr.domain.order.gateway.IPayOrderGateway;
import cn.fcr.domain.order.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.order.model.valobj.OrderVO;
import cn.fcr.domain.order.service.IMallOrderService;
import cn.fcr.domain.order.service.IOrderStateMachineService;
import cn.fcr.domain.order.service.PayOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 订单应用服务单元测试（④ B2 回归保护）
 *
 * <p>锁定超时关单与晚到支付的编排语义：
 * <ul>
 *   <li>订单不存在 → 忽略，不查询支付宝、不关单</li>
 *   <li>支付宝侧交易已成功 → 转履约路径，不关闭订单</li>
 *   <li>未支付 → 状态机关单 + 事务提交后恢复 Redis 预扣库存</li>
 *   <li>订单已关闭仍收到支付成功通知（晚到支付）→ 明确异常路径：不履约、不发布事件</li>
 * </ul>
 *
 * @author 傅崇睿
 */
@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private IMallCartService mallCartService;
    @Mock
    private IMallOrderService mallOrderService;
    @Mock
    private IOrderPaymentGateway orderPaymentGateway;
    @Mock
    private PayOrderService payOrderService;
    @Mock
    private IOrderEventPublisher orderEventPublisher;
    @Mock
    private IOrderStateMachineService orderStateMachineService;
    @Mock
    private IPayOrderGateway payOrderGateway;
    @Mock
    private OrderTransactionService orderTransactionService;
    @Mock
    private IIdempotentGateway idempotentGateway;
    @Mock
    private IMallOrderQueryGateway mallOrderQueryGateway;
    @Mock
    private IUserBindingGateway userBindingGateway;
    @Mock
    private IWeChatGateway weChatGateway;
    @Mock
    private IAlipayQueryGateway alipayQueryGateway;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    /** 构造指定状态的订单 VO */
    private OrderVO orderWithStatus(String orderNo, String status) {
        return OrderVO.builder().orderNo(orderNo).status(status).build();
    }

    @Test
    void handleTimeoutCloseOrder_orderNotFound_returnsFalse() {
        String orderNo = "ORD_MISSING";
        when(mallOrderService.getOrderByNo(orderNo)).thenReturn(null);

        assertFalse(orderApplicationService.handleTimeoutCloseOrder(orderNo));

        verifyNoInteractions(alipayQueryGateway);
        verify(orderTransactionService, never()).cancelOrderInTransaction(orderNo);
    }

    @Test
    void handleTimeoutCloseOrder_alipayTradeSuccess_rescueInsteadOfClose() {
        String orderNo = "ORD_RESCUED";
        // 第一次查询：超时关单入口；第二次查询：changeOrderPaySuccess 的晚到支付判断
        when(mallOrderService.getOrderByNo(orderNo))
                .thenReturn(orderWithStatus(orderNo, "INIT"))
                .thenReturn(orderWithStatus(orderNo, "INIT"));
        when(alipayQueryGateway.queryTradeSuccess(orderNo)).thenReturn(true);

        assertTrue(orderApplicationService.handleTimeoutCloseOrder(orderNo));

        // 已支付：走履约事务，绝不关单
        verify(orderTransactionService).changeOrderPaySuccessInTransaction(orderNo);
        verify(orderTransactionService, never()).cancelOrderInTransaction(orderNo);
        verify(orderEventPublisher).publishPaySuccess(null, orderNo);
    }

    @Test
    void handleTimeoutCloseOrder_unpaid_closesAndRestoresStock() {
        String orderNo = "ORD_CLOSE";
        when(mallOrderService.getOrderByNo(orderNo)).thenReturn(orderWithStatus(orderNo, "INIT"));
        when(alipayQueryGateway.queryTradeSuccess(orderNo)).thenReturn(false);
        when(orderTransactionService.cancelOrderInTransaction(orderNo)).thenReturn(true);

        assertTrue(orderApplicationService.handleTimeoutCloseOrder(orderNo));

        verify(orderTransactionService).cancelOrderInTransaction(orderNo);
        verify(orderStateMachineService).restoreStockForCancel(orderNo);
        verify(orderTransactionService, never()).changeOrderPaySuccessInTransaction(orderNo);
    }

    @Test
    void handleTimeoutCloseOrder_cancelRejected_noStockRestore() {
        String orderNo = "ORD_GUARD";
        when(mallOrderService.getOrderByNo(orderNo)).thenReturn(orderWithStatus(orderNo, "PAID"));
        when(alipayQueryGateway.queryTradeSuccess(orderNo)).thenReturn(false);
        when(orderTransactionService.cancelOrderInTransaction(orderNo)).thenReturn(false);

        assertFalse(orderApplicationService.handleTimeoutCloseOrder(orderNo));

        verify(orderStateMachineService, never()).restoreStockForCancel(orderNo);
    }

    @Test
    void changeOrderPaySuccess_orderCanceled_latePaidErrorPath() {
        String orderNo = "ORD_LATE";
        String tradeNo = "TRADE_LATE_001";
        when(mallOrderService.getOrderByNo(orderNo)).thenReturn(orderWithStatus(orderNo, "CANCELED"));

        orderApplicationService.changeOrderPaySuccess(orderNo, tradeNo);

        // 晚到支付：不履约、不发布支付成功事件（error 日志明确待人工介入）
        verify(orderTransactionService, never()).changeOrderPaySuccessInTransaction(orderNo);
        verify(orderEventPublisher, never()).publishPaySuccess(tradeNo, orderNo);
    }
}
