package cn.fcr.domain.mall.service.impl;

import cn.fcr.domain.mall.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.mall.gateway.IOrderPaymentGateway;
import cn.fcr.domain.mall.gateway.IStockGateway;
import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.model.entity.OrderItemEntity;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.mall.service.IOrderStateMachineService;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MallOrderServiceImpl implements IMallOrderService {

    private final IMallOrderQueryGateway mallOrderQueryGateway;
    private final IOrderPaymentGateway orderPaymentGateway;
    private final IOrderStateMachineService orderStateMachineService;
    private final IStockGateway stockGateway;

    public MallOrderServiceImpl(IMallOrderQueryGateway mallOrderQueryGateway,
                                IOrderPaymentGateway orderPaymentGateway,
                                IOrderStateMachineService orderStateMachineService,
                                IStockGateway stockGateway) {
        this.mallOrderQueryGateway = mallOrderQueryGateway;
        this.orderPaymentGateway = orderPaymentGateway;
        this.orderStateMachineService = orderStateMachineService;
        this.stockGateway = stockGateway;
    }

    @Override
    public List<CartItemVO> checkAndDeductStock(List<CartItemVO> cart) {
        List<CartItemVO> deductedItems = new ArrayList<>();

        // 逐项检查并扣减：边扣边记录，失败时在方法内部主动回滚已扣部分
        for (CartItemVO item : cart) {
            if (!hasEnoughStock(item.getProductId(), item.getQuantity())) {
                // 库存不足，回滚已扣减的库存后抛出异常
                restoreDeductedStock(deductedItems);
                throw new IllegalArgumentException("商品库存不足: productId=" + item.getProductId());
            }
            try {
                stockGateway.deductStock(item.getProductId(), item.getQuantity());
                deductedItems.add(item);
            } catch (Exception e) {
                // Redis 扣减异常，回滚已扣减的库存后抛出原始异常
                restoreDeductedStock(deductedItems);
                throw e;
            }
        }

        return deductedItems;
    }

    @Override
    public OrderEntity buildAndSaveOrder(Long userId, String address, List<CartItemVO> cart) {
        OrderEntity orderEntity = OrderEntity.createFromCart(userId, address, cart);
        mallOrderQueryGateway.saveOrder(orderEntity);
        return orderEntity;
    }

    @Override
    public void restoreDeductedStock(List<CartItemVO> deductedItems) {
        if (deductedItems == null || deductedItems.isEmpty()) {
            return;
        }
        for (CartItemVO item : deductedItems) {
            stockGateway.restoreStock(item.getProductId(), item.getQuantity());
            log.warn("【库存恢复】创建订单失败，恢复库存，productId={}, quantity={}",
                    item.getProductId(), item.getQuantity());
        }
    }

    @Override
    public List<OrderVO> listOrders(Long userId, String status, String start, String end) {
        return mallOrderQueryGateway.findOrders(userId, status, start, end);
    }

    @Override
    public int deleteOrder(Long id) {
        return mallOrderQueryGateway.deleteById(id);
    }

    @Override
    public int cancelOrder(Long orderId) {
        OrderEntity order = mallOrderQueryGateway.findById(orderId);
        if (order == null) {
            return 0;
        }
        boolean success = orderStateMachineService.cancel(order.getOrderNo());
        return success ? 1 : 0;
    }

    @Override
    public int deliverOrder(Long orderId) {
        OrderEntity order = mallOrderQueryGateway.findById(orderId);
        if (order == null) {
            return 0;
        }
        boolean success = orderStateMachineService.deliver(order.getOrderNo());
        return success ? 1 : 0;
    }

    @Override
    public void paySuccess(String orderNo) {
        boolean success = orderStateMachineService.paySuccess(orderNo);
        if (success) {
            log.info("订单支付成功，orderNo=" + orderNo);
        } else {
            log.warn("订单状态不允许更新或订单不存在，orderNo=" + orderNo + "，可能是重复回调");
        }
    }

    @Override
    public OrderVO getOrderByNo(String orderNo) {
        OrderEntity orderEntity = mallOrderQueryGateway.findByOrderNo(orderNo);
        if (orderEntity == null) return null;

        return OrderVO.builder()
                .orderNo(orderEntity.getOrderNo())
                .userId(orderEntity.getUserId())
                .totalAmount(orderEntity.getTotalAmount())
                .address(orderEntity.getAddress())
                .status(orderEntity.getState() != null ? orderEntity.getState().getCode() : "CREATED")
                .statusDesc(orderEntity.getState() != null ? orderEntity.getState().getDescription() : "")
                .createTime(orderEntity.getCreateTime())
                .updateTime(orderEntity.getUpdateTime())
                .build();
    }

    @Override
    public OrderCreateVO continuePay(String orderNo) {
        log.info("继续支付订单，orderNo=" + orderNo);

        // 1. 查询订单
        OrderEntity orderEntity = mallOrderQueryGateway.findByOrderNo(orderNo);
        if (orderEntity == null) {
            throw new IllegalArgumentException("订单不存在: " + orderNo);
        }

        // 2. 检查订单状态是否允许继续支付
        if (!orderEntity.canPay()) {
            throw new IllegalStateException("订单状态不允许支付: " + orderEntity.getState().getDescription());
        }

        // 3. 重新生成支付链接
        PayOrderEntity payOrderEntity = orderEntity.toPayOrder();
        String payUrl = orderPaymentGateway.generatePayUrl(payOrderEntity);

        // 4. 更新支付订单信息
        orderPaymentGateway.updatePayOrderInfo(payOrderEntity);

        log.info("继续支付订单成功，orderNo=" + orderNo);

        return OrderCreateVO.builder()
                .orderNo(orderEntity.getOrderNo())
                .totalAmount(orderEntity.getTotalAmount())
                .status(orderEntity.getState().getCode())
                .payUrl(payUrl)
                .build();
    }

    @Override
    public boolean checkOrderStock(String orderNo) {
        log.info("检查订单库存，orderNo=" + orderNo);

        OrderEntity orderEntity = mallOrderQueryGateway.findByOrderNo(orderNo);
        if (orderEntity == null) {
            log.warn("订单不存在，orderNo=" + orderNo);
            return false;
        }

        List<OrderItemEntity> items = orderEntity.getItems();
        if (items == null || items.isEmpty()) {
            log.warn("订单无商品项，orderNo=" + orderNo);
            return false;
        }

        for (OrderItemEntity item : items) {
            long currentStock = stockGateway.getStock(item.getProductId());
            if (currentStock < item.getQuantity()) {
                log.warn("库存不足，productId=" + item.getProductId() + ", 需要=" + item.getQuantity() + ", 当前=" + currentStock);
                return false;
            }
        }

        log.info("订单库存检查通过，orderNo=" + orderNo);
        return true;
    }

    /**
     * 预检查库存是否充足
     * 【DDD 重构】库存预检查下沉到 Domain 层，由业务逻辑自行判断，
     * Gateway 层只保留原子扣减操作和竞态补偿。
     *
     * @param productId 商品ID
     * @param quantity  需要数量
     * @return true=库存充足，false=库存不足
     */
    private boolean hasEnoughStock(Long productId, Integer quantity) {
        long currentStock = stockGateway.getStock(productId);
        log.info("【库存预检查】productId={}, 当前库存={}, 需要数量={}", productId, currentStock, quantity);
        return currentStock >= quantity;
    }
}
