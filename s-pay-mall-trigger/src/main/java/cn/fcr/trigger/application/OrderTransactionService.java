package cn.fcr.trigger.application;

import cn.fcr.domain.order.gateway.IOrderPaymentGateway;
import cn.fcr.domain.order.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.mall.cart.model.valobj.CartItemVO;
import cn.fcr.domain.order.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.cart.service.IMallCartService;
import cn.fcr.domain.order.service.IMallOrderService;
import cn.fcr.domain.order.legacy.service.IOrderService;
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
    /** 旧订单领域服务 */
    private final IOrderService orderService;
    /** 商城订单查询网关（用于区分新旧链订单） */
    private final IMallOrderQueryGateway mallOrderQueryGateway;

    public OrderTransactionService(IMallCartService mallCartService,
                                   IMallOrderService mallOrderService,
                                   IOrderPaymentGateway orderPaymentGateway,
                                   IOrderService orderService,
                                   IMallOrderQueryGateway mallOrderQueryGateway) {
        this.mallCartService = mallCartService;
        this.mallOrderService = mallOrderService;
        this.orderPaymentGateway = orderPaymentGateway;
        this.orderService = orderService;
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
     * <p>【JV-003 M1】按 order_main 是否存在分流：
     * 新订单（mall 链）直接走状态机，一步完成 order_main + pay_order + MySQL 库存扣减；
     * 遗留旧订单（仅 pay_order 行）保留旧链逻辑，仅更新 pay_order 状态。</p>
     *
     * @param orderId 订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeOrderPaySuccessInTransaction(String orderId) {
        OrderEntity mallOrder = mallOrderQueryGateway.findByOrderNo(orderId);
        if (mallOrder != null) {
            // 新链订单：状态机统一处理 order_main + pay_order + DB库存，天然幂等（重复回调返回 false）
            mallOrderService.paySuccess(orderId);
        } else {
            // 遗留旧链订单（仅 pay_order 行）：旧链仅更新 pay_order 状态
            orderService.changeOrderPaySuccess(orderId);
        }
    }
}
