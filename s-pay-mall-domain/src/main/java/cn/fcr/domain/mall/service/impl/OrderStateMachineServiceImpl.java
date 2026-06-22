package cn.fcr.domain.mall.service.impl;

import cn.fcr.domain.mall.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.mall.gateway.IPayOrderGateway;
import cn.fcr.domain.mall.gateway.IStockGateway;
import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.model.entity.OrderItemEntity;
import cn.fcr.domain.mall.model.entity.OrderState;
import cn.fcr.domain.mall.service.IOrderStateMachineService;
import cn.fcr.domain.shared.model.vo.PayStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * 订单状态机服务实现，封装状态转换业务规则，确保 order_main 和 pay_order 状态一致性。
 * 支付成功后同步扣减 MySQL 库存（最终一致性）。
 *
 * @author 傅崇睿
 */
@Slf4j
public class OrderStateMachineServiceImpl implements IOrderStateMachineService {

    private final IMallOrderQueryGateway mallOrderQueryGateway;
    private final IPayOrderGateway payOrderGateway;
    private final IStockGateway stockGateway;

    public OrderStateMachineServiceImpl(IMallOrderQueryGateway mallOrderQueryGateway,
                                        IPayOrderGateway payOrderGateway,
                                        IStockGateway stockGateway) {
        this.mallOrderQueryGateway = mallOrderQueryGateway;
        this.payOrderGateway = payOrderGateway;
        this.stockGateway = stockGateway;
    }

    @Override
    public boolean paySuccess(String orderNo) {
        log.info("【状态机】处理支付成功，orderNo=" + orderNo);

        OrderEntity order = mallOrderQueryGateway.findByOrderNo(orderNo);
        if (order == null) {
            log.warn("【状态机】订单不存在，orderNo=" + orderNo);
            return false;
        }

        if (!order.canPay()) {
            log.warn("【状态机】订单状态不允许支付，orderNo=" + orderNo + ", 当前状态=" + order.getState());
            return false;
        }

        // 更新 order_main 状态为 PAID
        int orderUpdated = mallOrderQueryGateway.updateOrderStatusByOrderNo(orderNo, OrderState.PAID.getCode());
        
        // 更新 pay_order 状态为 PAID
        payOrderGateway.updatePayStatusToPaid(orderNo);

        // 同步扣减 MySQL 库存（确保 Redis 预扣结果持久化到 DB）
        syncDBStockForPaySuccess(order);

        log.info("【状态机】支付成功状态更新完成，orderNo=" + orderNo);
        return orderUpdated > 0;
    }

    /**
     * 同步扣减 MySQL 数据库库存
     * 支付成功后调用，确保 Redis 预扣结果持久化到 MySQL
     * 遍历订单项，对每个商品执行乐观锁扣减
     *
     * @param order 订单实体
     */
    private void syncDBStockForPaySuccess(OrderEntity order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            log.warn("【库存同步】订单无子项，跳过库存同步，orderNo=" + order.getOrderNo());
            return;
        }

        for (OrderItemEntity item : order.getItems()) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            if (productId == null || quantity == null) {
                log.warn("【库存同步】订单项数据异常，跳过，orderNo=" + order.getOrderNo() + ", item=" + item);
                continue;
            }

            boolean success = stockGateway.syncDBStockDeduct(productId, quantity);
            if (!success) {
                log.warn("【库存同步】DB库存扣减失败，productId=" + productId + ", quantity=" + quantity + ", orderNo=" + order.getOrderNo());
                // 注意：这里不抛出异常，因为 Redis 已预扣成功，DB 扣减失败可能是并发场景下的正常情况
                // 可以考虑加入补偿机制或告警通知
            }
        }
    }

    @Override
    public boolean deliver(String orderNo) {
        log.info("【状态机】处理发货，orderNo=" + orderNo);

        OrderEntity order = mallOrderQueryGateway.findByOrderNo(orderNo);
        if (order == null) {
            log.warn("【状态机】订单不存在，orderNo=" + orderNo);
            return false;
        }

        if (!order.canDeliver()) {
            log.warn("【状态机】订单状态不允许发货，orderNo=" + orderNo + ", 当前状态=" + order.getState());
            return false;
        }

        // 更新 order_main 状态为 SHIPPED
        int orderUpdated = mallOrderQueryGateway.updateOrderStatusByOrderNo(orderNo, OrderState.SHIPPED.getCode());
        
        // 更新 pay_order 状态为 TRADE_DONE
        payOrderGateway.updatePayStatusToTradeDone(orderNo);

        log.info("【状态机】发货状态更新完成，orderNo=" + orderNo);
        return orderUpdated > 0;
    }

    @Override
    public boolean complete(String orderNo) {
        log.info("【状态机】处理订单完成，orderNo=" + orderNo);

        OrderEntity order = mallOrderQueryGateway.findByOrderNo(orderNo);
        if (order == null) {
            log.warn("【状态机】订单不存在，orderNo=" + orderNo);
            return false;
        }

        if (!order.canComplete()) {
            log.warn("【状态机】订单状态不允许完成，orderNo=" + orderNo + ", 当前状态=" + order.getState());
            return false;
        }

        int updated = mallOrderQueryGateway.updateOrderStatusByOrderNo(orderNo, OrderState.DONE.getCode());
        log.info("【状态机】订单完成状态更新完成，orderNo=" + orderNo);
        return updated > 0;
    }

    @Override
    public boolean cancel(String orderNo) {
        log.info("【状态机】处理取消订单，orderNo=" + orderNo);

        OrderEntity order = mallOrderQueryGateway.findByOrderNo(orderNo);
        if (order == null) {
            log.warn("【状态机】订单不存在，orderNo=" + orderNo);
            return false;
        }

        if (!order.canCancel()) {
            log.warn("【状态机】订单状态不允许取消，orderNo=" + orderNo + ", 当前状态=" + order.getState());
            return false;
        }

        // 在更新状态之前判断订单是否已支付（用于后续库存恢复判断）
        boolean isPaid = order.getState() == OrderState.PAID;

        // 更新 order_main 状态为 CANCELED
        int orderUpdated = mallOrderQueryGateway.updateOrderStatusByOrderNo(orderNo, OrderState.CANCELED.getCode());
        
        // 更新 pay_order 状态为 CLOSED（仅当未支付时）
        PayStatus currentPayStatus = payOrderGateway.getPayStatus(orderNo);
        if (currentPayStatus == PayStatus.WAIT_PAY || currentPayStatus == PayStatus.PAYING) {
            payOrderGateway.closePayOrder(orderNo);
        }

        // 恢复库存（用户主动取消订单，需恢复预扣的库存）
        restoreStockForCancel(order, isPaid);

        log.info("【状态机】取消订单状态更新完成，orderNo=" + orderNo);
        return orderUpdated > 0;
    }

    /**
     * 恢复库存（取消订单）
     * 用户主动取消订单时调用，恢复 Redis 和 MySQL 中已扣减的库存
     *
     * @param order 订单实体
     * @param isPaid 是否已支付（已支付的订单需要恢复 MySQL 库存）
     */
    private void restoreStockForCancel(OrderEntity order, boolean isPaid) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            log.warn("【库存恢复】订单无子项，跳过库存恢复，orderNo=" + order.getOrderNo());
            return;
        }

        for (OrderItemEntity item : order.getItems()) {
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();

            if (productId == null || quantity == null) {
                continue;
            }

            // 恢复 Redis 库存
            stockGateway.restoreStock(productId, quantity);
            log.info("【库存恢复】Redis库存已恢复，productId=" + productId + ", quantity=" + quantity + ", orderNo=" + order.getOrderNo());

            // 如果订单已支付，还需要恢复 MySQL 库存
            if (isPaid) {
                boolean dbRestoreSuccess = stockGateway.syncDBStockRestore(productId, quantity);
                if (dbRestoreSuccess) {
                    log.info("【库存恢复】MySQL库存已恢复，productId=" + productId + ", quantity=" + quantity + ", orderNo=" + order.getOrderNo());
                } else {
                    log.warn("【库存恢复】MySQL库存恢复失败，productId=" + productId + ", quantity=" + quantity + ", orderNo=" + order.getOrderNo());
                }
            }
        }
    }
}