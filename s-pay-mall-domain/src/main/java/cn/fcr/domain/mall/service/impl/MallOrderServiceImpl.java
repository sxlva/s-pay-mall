package cn.fcr.domain.mall.service.impl;

import cn.fcr.domain.mall.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.mall.gateway.IOrderPaymentGateway;
import cn.fcr.domain.mall.gateway.IStockGateway;
import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.model.entity.OrderItemEntity;
import cn.fcr.domain.mall.model.entity.OrderState;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.service.IMallCartService;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.mall.service.IOrderStateMachineService;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MallOrderServiceImpl implements IMallOrderService {

    @Resource
    private IMallCartService mallCartService;

    @Resource
    private IMallOrderQueryGateway mallOrderQueryGateway;

    @Resource
    private IOrderPaymentGateway orderPaymentGateway;

    @Resource
    private IOrderStateMachineService orderStateMachineService;

    @Resource
    private IStockGateway stockGateway;

    @Override
    public OrderCreateVO createOrder(Long userId, String address) {
        List<CartItemVO> cart = mallCartService.listCart(userId);
        List<CartItemVO> deductedItems = new ArrayList<>();
        
        try {
            // 预扣减库存：遍历购物车商品，执行 Redis decr 操作
            for (CartItemVO item : cart) {
                stockGateway.deductStock(item.getProductId(), item.getQuantity());
                deductedItems.add(item);
            }

            OrderEntity orderEntity = OrderEntity.createFromCart(userId, address, cart);
            mallOrderQueryGateway.saveOrder(orderEntity);

            mallCartService.clearCart(userId);

            orderPaymentGateway.sendDelayCloseMessage(orderEntity.getOrderNo());

            PayOrderEntity payOrderEntity = orderEntity.toPayOrder();
            String payUrl = orderPaymentGateway.generatePayUrl(payOrderEntity);

            orderPaymentGateway.updatePayOrderInfo(payOrderEntity);

            return OrderCreateVO.builder()
                    .orderNo(orderEntity.getOrderNo())
                    .totalAmount(orderEntity.getTotalAmount())
                    .status(orderEntity.getState().getCode())
                    .payUrl(payUrl)
                    .build();
        } catch (Exception e) {
            // 发生异常时恢复已扣减的库存
            for (CartItemVO item : deductedItems) {
                stockGateway.restoreStock(item.getProductId(), item.getQuantity());
                log.warn("【库存恢复】创建订单失败，恢复库存，productId={}, quantity={}", 
                        item.getProductId(), item.getQuantity());
            }
            throw e;
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
            log.info("订单支付成功，orderNo={}", orderNo);
        } else {
            log.warn("订单状态不允许更新或订单不存在，orderNo={}，可能是重复回调", orderNo);
        }
    }

    private String mapDomainStateToDbStatus(OrderState state) {
        if (state == null) return "CREATED";
        switch (state) {
            case INIT: return "CREATED";
            case PAID: return "PAID";
            case SHIPPED: return "SHIPPED";
            case DONE: return "COMPLETED";
            case CANCELED: return "CANCELLED";
            default: return state.getCode();
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
        log.info("继续支付订单，orderNo={}", orderNo);

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

        log.info("继续支付订单成功，orderNo={}", orderNo);

        return OrderCreateVO.builder()
                .orderNo(orderEntity.getOrderNo())
                .totalAmount(orderEntity.getTotalAmount())
                .status(orderEntity.getState().getCode())
                .payUrl(payUrl)
                .build();
    }

    @Override
    public boolean checkOrderStock(String orderNo) {
        log.info("检查订单库存，orderNo={}", orderNo);

        OrderEntity orderEntity = mallOrderQueryGateway.findByOrderNo(orderNo);
        if (orderEntity == null) {
            log.warn("订单不存在，orderNo={}", orderNo);
            return false;
        }

        List<OrderItemEntity> items = orderEntity.getItems();
        if (items == null || items.isEmpty()) {
            log.warn("订单无商品项，orderNo={}", orderNo);
            return false;
        }

        for (OrderItemEntity item : items) {
            long currentStock = stockGateway.getStock(item.getProductId());
            if (currentStock < item.getQuantity()) {
                log.warn("库存不足，productId={}, 需要={}, 当前={}", 
                        item.getProductId(), item.getQuantity(), currentStock);
                return false;
            }
        }

        log.info("订单库存检查通过，orderNo={}", orderNo);
        return true;
    }
}
