package cn.fcr.trigger.application;

import cn.fcr.domain.mall.gateway.IOrderPaymentGateway;
import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.service.IMallCartService;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单应用层服务
 * <p>
 * 【DDD 应用层】负责事务编排，协调领域对象完成业务操作。
 * 事务边界统一在此层控制，领域层不感知事务。
 * <p>
 * 当前仅覆盖"新 Mall Domain"链路（IMallCartService + IMallOrderService）。
 * 旧 Order Domain（IOrderService / PayOrderService）调用链、事件发布剥离
 * 留到后续批次处理。
 * <p>
 * 已知遗留项 P0-2：Application 层理想上应独立于 Trigger 模块，
 * 当前因避免循环依赖暂留在 trigger 模块，后续可新建独立 application 模块解决。
 */
@Slf4j
@Service
public class OrderApplicationService {

    private final IMallCartService mallCartService;
    private final IMallOrderService mallOrderService;
    private final IOrderPaymentGateway orderPaymentGateway;

    public OrderApplicationService(IMallCartService mallCartService,
                                   IMallOrderService mallOrderService,
                                   IOrderPaymentGateway orderPaymentGateway) {
        this.mallCartService = mallCartService;
        this.mallOrderService = mallOrderService;
        this.orderPaymentGateway = orderPaymentGateway;
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

    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO createOrder(Long userId, String address) {
        List<CartItemVO> cart = mallCartService.listCart(userId);
        List<CartItemVO> deductedItems = new ArrayList<>();
        try {
            deductedItems = mallOrderService.checkAndDeductStock(cart);
            OrderEntity orderEntity = mallOrderService.buildAndSaveOrder(userId, address, cart);

            orderPaymentGateway.sendDelayCloseMessage(orderEntity.getOrderNo());
            PayOrderEntity payOrderEntity = orderEntity.toPayOrder();
            String payUrl = orderPaymentGateway.generatePayUrl(payOrderEntity);
            orderPaymentGateway.updatePayOrderInfo(payOrderEntity);

            mallCartService.clearCart(userId);

            return OrderCreateVO.builder()
                    .orderNo(orderEntity.getOrderNo())
                    .totalAmount(orderEntity.getTotalAmount())
                    .status(orderEntity.getState().getCode())
                    .payUrl(payUrl)
                    .build();
        } catch (Exception e) {
            mallOrderService.restoreDeductedStock(deductedItems);
            throw e;
        }
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
}
