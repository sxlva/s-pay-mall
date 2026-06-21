package cn.fcr.trigger.application;

import cn.fcr.domain.mall.gateway.IOrderPaymentGateway;
import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.service.IMallCartService;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.order.service.IOrderService;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单事务服务
 * <p>
 * 【Trigger 应用层】负责订单创建的事务性 DB 操作。
 * 将事务边界与 MQ 发送解耦，避免事务 hold 问题。
 * <p>
 * 仅被 OrderApplicationService 内部调用，不对外暴露。
 */
@Slf4j
@Service
class OrderTransactionService {

    private final IMallCartService mallCartService;
    private final IMallOrderService mallOrderService;
    private final IOrderPaymentGateway orderPaymentGateway;
    private final IOrderService orderService;

    public OrderTransactionService(IMallCartService mallCartService,
                                   IMallOrderService mallOrderService,
                                   IOrderPaymentGateway orderPaymentGateway,
                                   IOrderService orderService) {
        this.mallCartService = mallCartService;
        this.mallOrderService = mallOrderService;
        this.orderPaymentGateway = orderPaymentGateway;
        this.orderService = orderService;
    }

    /**
     * 在事务内完成订单创建的所有 DB 操作
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
     * @param orderId 订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeOrderPaySuccessInTransaction(String orderId) {
        orderService.changeOrderPaySuccess(orderId);
    }
}
