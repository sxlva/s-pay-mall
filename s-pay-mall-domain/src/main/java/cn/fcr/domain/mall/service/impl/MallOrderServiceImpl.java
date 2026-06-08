package cn.fcr.domain.mall.service.impl;

import cn.fcr.domain.mall.gateway.IMallOrderQueryGateway;
import cn.fcr.domain.mall.gateway.IOrderPaymentGateway;
import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.model.entity.OrderState;
import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.mall.service.IMallCartService;
import cn.fcr.domain.mall.service.IMallOrderService;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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

    @Override
    public OrderCreateVO createOrder(Long userId, String address) {
        List<CartItemVO> cart = mallCartService.listCart(userId);
        
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
        return mallOrderQueryGateway.updateOrderStatus(orderId, mapDomainStateToDbStatus(OrderState.CANCELED));
    }

    @Override
    public int deliverOrder(Long orderId) {
        OrderEntity order = mallOrderQueryGateway.findById(orderId);
        if (order != null && OrderState.PAID.getCode().equals(order.getState().getCode())) {
            return mallOrderQueryGateway.updateOrderStatus(orderId, mapDomainStateToDbStatus(OrderState.SHIPPED));
        }
        return 0;
    }

    @Override
    public void paySuccess(String orderNo) {
        int updated = mallOrderQueryGateway.updateOrderStatusByOrderNo(orderNo, OrderState.PAID.getCode());
        if (updated > 0) {
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
}
