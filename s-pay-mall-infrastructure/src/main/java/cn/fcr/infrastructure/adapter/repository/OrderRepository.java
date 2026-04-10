package cn.fcr.infrastructure.adapter.repository;

import cn.fcr.domain.order.adapter.repository.IOrderRepository;
import cn.fcr.domain.order.model.aggregate.CreateOrderAggregate;
import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.entity.ShopCartEntity;
import cn.fcr.domain.order.model.valobj.OrderStatusVO;
import cn.fcr.infrastructure.dao.IOrderDao;
import cn.fcr.infrastructure.dao.po.PayOrder;
import cn.fcr.types.event.BaseEvent;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description 订单仓储服务实现
 * @author 傅崇睿
 * @date 2025/8/2
 */
@Slf4j
@Repository
public class OrderRepository implements IOrderRepository {

    @Resource
    private IOrderDao orderDao;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity) {
        PayOrder payOrderReq = new PayOrder();
        payOrderReq.setUserId(shopCartEntity.getUserId());
        payOrderReq.setProductId(shopCartEntity.getProductId());
        PayOrder payOrder = orderDao.queryUnPayOrder(payOrderReq);
        if (null == payOrder) return null;

        return OrderEntity.builder()
                .orderId(payOrder.getOrderId())
                .productName(payOrder.getProductName())
                .totalAmount(payOrder.getTotalAmount())
                .orderStatusVO(OrderStatusVO.getVO(payOrder.getStatus()))
                .payUrl(payOrder.getPayUrl())
                .build();
    }

    @Override
    public void doSaveOrder(CreateOrderAggregate orderAggregate) {
        PayOrder payOrder = new PayOrder();
        payOrder.setUserId(orderAggregate.getUserId());
        payOrder.setProductId(orderAggregate.getProductEntity().getProductId());
        payOrder.setProductName(orderAggregate.getProductEntity().getProductName());
        payOrder.setOrderId(orderAggregate.getOrderEntity().getOrderId());
        payOrder.setOrderTime(orderAggregate.getOrderEntity().getOrderTime());
        payOrder.setTotalAmount(orderAggregate.getProductEntity().getPrice());
        payOrder.setStatus(orderAggregate.getOrderEntity().getOrderStatusVO().getCode());
        orderDao.insert(payOrder);
    }

    @Override
    public void updateOrderPayInfo(PayOrderEntity payOrderEntity) {
        PayOrder payOrder = new PayOrder();
        payOrder.setOrderId(payOrderEntity.getOrderId());
        payOrder.setPayUrl(payOrderEntity.getPayUrl());
        payOrder.setStatus(payOrderEntity.getOrderStatus().getCode());
        orderDao.updateOrderPayInfo(payOrder);
    }

    @Override
    public void changeOrderPaySuccess(String orderId) {
        PayOrder payOrder = new PayOrder();
        payOrder.setOrderId(orderId);
        payOrder.setStatus(OrderStatusVO.PAY_SUCCESS.getCode());
        orderDao.changeOrderPaySuccess(payOrder);
    }

    @Override
    public List<String> queryNoPayNotifyOrder() {
        return orderDao.queryNoPayNotifyOrder();
    }

    @Override
    public List<String> queryTimeoutCloseOrderList() {
        return orderDao.queryTimeoutCloseOrderList();
    }

    @Override
    public boolean changeOrderClose(String orderId) {
        return orderDao.changeOrderClose();
    }

    @Override
    public PayOrderEntity queryOrderById(String orderId) {
        PayOrder payOrder = orderDao.queryOrderById(orderId);
        if (null == payOrder) return null;
        return PayOrderEntity.builder()
                .userId(payOrder.getUserId())
                .productId(payOrder.getProductId())
                .productName(payOrder.getProductName())
                .orderId(payOrder.getOrderId())
                .orderTime(payOrder.getOrderTime())
                .totalAmount(payOrder.getTotalAmount())
                .orderStatus(OrderStatusVO.getVO(payOrder.getStatus()))
                .build();
    }

    @Override
    public void publishEvent(BaseEvent.EventMessage<?> eventMessage) {
        try {
            String message = JSON.toJSONString(eventMessage);
            log.info("发送MQ消息 topic: {} message: {}", "pay_success", message);
            rabbitTemplate.convertAndSend("pay_success", message);
        } catch (Exception e) {
            log.error("发送MQ消息失败", e);
        }
    }
}
