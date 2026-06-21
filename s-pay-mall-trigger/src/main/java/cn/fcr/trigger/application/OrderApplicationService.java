package cn.fcr.trigger.application;

import cn.fcr.domain.mall.gateway.IOrderPaymentGateway;
import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.service.IMallCartService;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.order.adapter.event.IOrderEventPublisher;
import cn.fcr.domain.order.model.entity.ShopCartEntity;
import cn.fcr.domain.order.service.IOrderService;
import cn.fcr.domain.order.service.PayOrderService;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 订单应用层服务
 * <p>
 * 【DDD 应用层】负责事务编排，协调领域对象完成业务操作。
 * 事务边界统一在此层控制，领域层不感知事务。
 * <p>
 * 已覆盖完整订单生命周期：购物车管理、订单创建、支付、发货、取消、超时关闭。
 * 包含 Mall Domain（新域）和 Order Domain（旧域）的统一收口。
 * <p>
 * 已知遗留项：Application 层理想上应独立于 Trigger 模块，
 * 当前因避免循环依赖暂留在 trigger 模块，后续可新建独立 application 模块解决。
 */
@Slf4j
@Service
public class OrderApplicationService {

    private final IMallCartService mallCartService;
    private final IMallOrderService mallOrderService;
    private final IOrderPaymentGateway orderPaymentGateway;
    private final IOrderService orderService;
    private final PayOrderService payOrderService;
    private final IOrderEventPublisher orderEventPublisher;
    private final OrderTransactionService orderTransactionService;

    public OrderApplicationService(IMallCartService mallCartService,
                                   IMallOrderService mallOrderService,
                                   IOrderPaymentGateway orderPaymentGateway,
                                   IOrderService orderService,
                                   PayOrderService payOrderService,
                                   IOrderEventPublisher orderEventPublisher,
                                   OrderTransactionService orderTransactionService) {
        this.mallCartService = mallCartService;
        this.mallOrderService = mallOrderService;
        this.orderPaymentGateway = orderPaymentGateway;
        this.orderService = orderService;
        this.payOrderService = payOrderService;
        this.orderEventPublisher = orderEventPublisher;
        this.orderTransactionService = orderTransactionService;
    }

    // ==================== 购物车 ====================

    @Transactional(rollbackFor = Exception.class)
    public int addCart(Long userId, Long productId, Integer quantity) {
        return mallCartService.addCart(userId, productId, quantity);
    }

    public List<CartItemVO> listCart(Long userId) {
        return mallCartService.listCart(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateCartQuantity(Long userId, Long productId, Integer quantity) {
        return mallCartService.updateQuantity(userId, productId, quantity);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteCartItem(Long userId, Long cartItemId) {
        return mallCartService.deleteCartItem(userId, cartItemId);
    }

    // ==================== 订单 ====================

    /**
     * 创建订单
     * <p>
     * 事务边界在 OrderTransactionService 中控制，MQ 消息发送在事务外执行，
     * 避免 sendDelayCloseMessage 在事务内部导致事务 hold 问题。
     */
    public OrderCreateVO createOrder(Long userId, String address) {
        // 事务内完成所有 DB 操作
        OrderCreateVO result = orderTransactionService.createOrderInTransaction(userId, address);

        // 事务提交后发送延时关闭消息（失败不影响主流程）
        try {
            orderPaymentGateway.sendDelayCloseMessage(result.getOrderNo());
        } catch (Exception e) {
            log.warn("发送延时关闭消息失败，orderNo: {}, error: {}", result.getOrderNo(), e.getMessage());
        }

        return result;
    }

    public List<OrderVO> listOrders(Long userId, String status, String startTime, String endTime) {
        return mallOrderService.listOrders(userId, status, startTime, endTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO continuePay(String orderNo) {
        return mallOrderService.continuePay(orderNo);
    }

    public boolean checkStock(String orderNo) {
        return mallOrderService.checkOrderStock(orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo) {
        mallOrderService.paySuccess(orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deliverOrder(Long orderId) {
        return mallOrderService.deliverOrder(orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int cancelOrder(Long orderId) {
        return mallOrderService.cancelOrder(orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteOrder(Long orderId) {
        return mallOrderService.deleteOrder(orderId);
    }

    // ==================== 旧 Order Domain ====================

    public PayOrderEntity createPayOrder(String userId, String productId) throws Exception {
        ShopCartEntity shopCartEntity = ShopCartEntity.builder()
                .userId(userId)
                .productId(productId)
                .build();
        return orderService.createOrder(shopCartEntity);
    }

    public boolean verifyPayCallbackSign(Map<String, String> params, String alipayPublicKey) {
        return payOrderService.verifyCallbackSign(params, alipayPublicKey);
    }

    /**
     * 订单支付成功处理
     * <p>
     * 事务边界在 OrderTransactionService 中控制，事件发布在事务外执行。
     */
    public void changeOrderPaySuccess(String orderId) {
        // 事务内完成 DB 状态更新
        orderTransactionService.changeOrderPaySuccessInTransaction(orderId);

        // 事务提交后发布支付成功事件（失败不影响主流程）
        try {
            orderEventPublisher.publishPaySuccess(orderId, orderId);
        } catch (Exception e) {
            log.warn("发布支付成功事件失败，orderId: {}, error: {}", orderId, e.getMessage());
        }
    }

    public List<String> queryNoPayNotifyOrder() {
        return orderService.queryNoPayNotifyOrder();
    }

    public List<String> queryTimeoutCloseOrderList() {
        return orderService.queryTimeoutCloseOrderList();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean changeOrderClose(String orderId) {
        return orderService.changeOrderClose(orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean handleTimeoutCloseOrder(String orderNo) {
        return orderService.handleTimeoutCloseOrder(orderNo);
    }

    // ==================== 跨域共享（Mall Domain 查询） ====================

    public OrderVO getOrderByNo(String orderNo) {
        return mallOrderService.getOrderByNo(orderNo);
    }
}
