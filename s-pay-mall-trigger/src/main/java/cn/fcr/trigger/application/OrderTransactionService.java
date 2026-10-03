package cn.fcr.trigger.application;

import cn.fcr.domain.order.gateway.IOrderPaymentGateway;
import cn.fcr.domain.order.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.mall.cart.model.valobj.CartItemVO;
import cn.fcr.domain.order.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.cart.service.IMallCartService;
import cn.fcr.domain.order.service.IMallOrderService;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单事务服务
 *
 * @author 傅崇睿
 */
@Slf4j
@Service
class OrderTransactionService {

    /** 商城购物车领域服务 */
    private final IMallCartService mallCartService;
    /** 商城订单领域服务 */
    private final IMallOrderService mallOrderService;
    /** 订单支付网关 */
    private final IOrderPaymentGateway orderPaymentGateway;
    /** 商城订单查询网关（用于区分新旧链订单） */
    private final IMallOrderQueryGateway mallOrderQueryGateway;

    public OrderTransactionService(IMallCartService mallCartService,
                                   IMallOrderService mallOrderService,
                                   IOrderPaymentGateway orderPaymentGateway,
                                   IMallOrderQueryGateway mallOrderQueryGateway) {
        this.mallCartService = mallCartService;
        this.mallOrderService = mallOrderService;
        this.orderPaymentGateway = orderPaymentGateway;
        this.mallOrderQueryGateway = mallOrderQueryGateway;
    }

    /**
     * 在事务内完成订单创建的所有 DB 操作
     *
     * <p>包含库存预扣、订单落库、支付单生成、购物车清空。
     * 发生异常时自动回滚库存预扣。</p>
     *
     * @param userId  用户ID
     * @param address 收货地址
     * @return 订单创建结果，包含 orderNo 和 payUrl
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO createOrderInTransaction(Long userId, String address) {
        List<CartItemVO> cart = mallCartService.listCart(userId);
        List<CartItemVO> deductedItems = new ArrayList<>();
        try {
            deductedItems = mallOrderService.checkAndDeductStock(cart);
            OrderEntity orderEntity = mallOrderService.buildAndSaveOrder(userId, address, cart);

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
            try {
                mallOrderService.restoreDeductedStock(deductedItems);
            } catch (Exception restoreEx) {
                log.error("库存回滚失败，需人工处理: items={}, error={}", deductedItems, restoreEx.getMessage());
            }
            throw e; // 始终抛出原始异常
        }
    }

    /**
     * 在事务内完成订单支付成功的 DB 状态更新
     *
     * <p>旧链已下线（legacy sunset）：订单一律走状态机，
     * 一步完成 order_main + pay_order + MySQL 库存扣减，天然幂等。
     * order_main 不存在的订单视为异常数据（不应再产生），记 warn 后返回。</p>
     *
     * @param orderId 订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeOrderPaySuccessInTransaction(String orderId) {
        OrderEntity mallOrder = mallOrderQueryGateway.findByOrderNo(orderId);
        if (mallOrder == null) {
            log.warn("支付回调：order_main 中不存在该订单，可能为旧链残留数据，orderId={}", orderId);
            return;
        }
        // 状态机统一处理 order_main + pay_order + DB库存，重复回调返回 false（幂等）
        mallOrderService.paySuccess(orderId);
    }
}
