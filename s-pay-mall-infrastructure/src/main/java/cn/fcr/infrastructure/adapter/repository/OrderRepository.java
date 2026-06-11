package cn.fcr.infrastructure.adapter.repository;

import cn.fcr.domain.order.adapter.event.PaySuccessMessageEvent;
import cn.fcr.domain.order.adapter.repository.IOrderRepository;
import cn.fcr.domain.order.model.aggregate.CreateOrderAggregate;
import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.entity.ProductEntity;
import cn.fcr.domain.order.model.entity.ShopCartEntity;
import cn.fcr.domain.order.model.valobj.OrderStatusVO;
import cn.fcr.infrastructure.dao.IOrderDao;
import cn.fcr.infrastructure.dao.IOrderMainDao;
import cn.fcr.infrastructure.dao.po.PayOrder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 傅崇睿
 * @date 2025/7/28 22:27
 * @description 订单仓储实现，负责订单落库、支付状态更新与支付事件投递
 */
@Slf4j
@Repository
public class OrderRepository implements IOrderRepository {

    @Resource
    private IOrderDao orderDao;
    @Resource
    private IOrderMainDao orderMainDao;
    @Resource
    private PaySuccessMessageEvent paySuccessMessageEvent;
    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void doSaveOrder(CreateOrderAggregate orderAggregate) {
        String userId = orderAggregate.getUserId();
        ProductEntity productEntity = orderAggregate.getProductEntity();
        OrderEntity orderEntity = orderAggregate.getOrderEntity();

        PayOrder order = new PayOrder();
        order.setUserId(userId);
        order.setProductId(productEntity.getProductId());
        order.setProductName(productEntity.getProductName());
        order.setOrderId(orderEntity.getOrderId());
        order.setOrderTime(orderEntity.getOrderTime());
        order.setTotalAmount(productEntity.getPrice());
        order.setStatus(orderEntity.getOrderStatusVO().getCode());

        orderDao.insert(order);
    }

    @Override
    public void updateOrderPayInfo(PayOrderEntity payOrderEntity) {
        PayOrder payOrderReq = PayOrder.builder()
                .userId(payOrderEntity.getUserId())
                .orderId(payOrderEntity.getOrderNo())
                .status(payOrderEntity.getStatus().getCode())
                .payUrl(payOrderEntity.getPayUrl())
                .build();
        orderDao.updateOrderPayInfo(payOrderReq);
        log.info("更新支付信息: userId={} orderNo={} payUrl={}",
                payOrderEntity.getUserId(),
                payOrderEntity.getOrderNo(),
                payOrderEntity.getPayUrl());
    }

    @Override
    public void changeOrderPaySuccess(String orderId) {
        PayOrder payOrderReq = new PayOrder();
        payOrderReq.setOrderId(orderId);
        payOrderReq.setStatus(OrderStatusVO.PAY_SUCCESS.getCode());
        orderDao.changeOrderPaySuccess(payOrderReq);
        log.info("pay_order 表状态已更新为 PAY_SUCCESS: orderId={}", orderId);

        PaySuccessMessageEvent.PaySuccessMessage paySuccessMessage = PaySuccessMessageEvent.PaySuccessMessage.builder()
                .tradeNo(orderId)
                .orderNo(orderId)
                .build();
        
        if (rocketMQTemplate != null) {
            try {
                rocketMQTemplate.convertAndSend("order_paid", paySuccessMessage);
                log.info("已发送支付成功消息到 RocketMQ: orderId={}", orderId);
            } catch (Exception e) {
                log.error("发送支付成功消息失败，但不影响订单状态更新: orderId={}, error={}", orderId, e.getMessage());
            }
        }
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
        orderDao.changeOrderClose(orderId);
        return true;
    }

    @Override
    public OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity) {
        PayOrder orderReq = new PayOrder();
        orderReq.setUserId(shopCartEntity.getUserId());
        orderReq.setProductId(shopCartEntity.getProductId());

        PayOrder order = orderDao.queryUnPayOrder(orderReq);
        if (null == order) return null;

        return OrderEntity.builder()
                .productId(order.getProductId())
                .productName(order.getProductName())
                .orderId(order.getOrderId())
                .orderStatusVO(OrderStatusVO.valueOf(order.getStatus()))
                .orderTime(order.getOrderTime())
                .totalAmount(order.getTotalAmount())
                .payUrl(order.getPayUrl())
                .build();
    }

    @Override
    public String queryOrderStatus(String orderNo) {
        return orderDao.queryOrderStatus(orderNo);
    }

    @Override
    public List<Map<String, Object>> queryOrderItems(String orderNo) {
        List<PayOrder> orders = orderDao.queryOrderByOrderNo(orderNo);
        return orders.stream()
                .map(order -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", order.getProductId());
                    item.put("quantity", 1);
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean closeOrderWithOptimisticLock(String orderNo, String expectStatus) {
        return orderDao.closeOrderWithOptimisticLock(orderNo, expectStatus) > 0;
    }

    @Override
    public OrderEntity findByOrderNo(String orderNo) {
        PayOrder order = orderDao.queryByOrderNo(orderNo);
        if (order == null) {
            return null;
        }
        // 【DDD】将 PO 转换为 Entity，确保领域对象完整性
        return OrderEntity.builder()
                .productId(order.getProductId())
                .productName(order.getProductName())
                .orderId(order.getOrderId())
                .orderStatusVO(OrderStatusVO.valueOf(order.getStatus()))
                .orderTime(order.getOrderTime())
                .totalAmount(order.getTotalAmount())
                .payUrl(order.getPayUrl())
                .build();
    }

    @Override
    public void save(OrderEntity order) {
        // 【DDD】仅保存聚合根的状态变更，Entity 的内部逻辑已处理好状态
        orderDao.updateStatus(order.getOrderId(), order.getOrderStatusVO().getCode());
        log.info("【DDD 持久化】订单状态已保存: orderNo={}, status={}",
                order.getOrderId(), order.getSafeStateCode());
    }

    @Override
    public long countByUserId(Long userId) {
        LambdaQueryWrapper<cn.fcr.infrastructure.dao.po.OrderMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(cn.fcr.infrastructure.dao.po.OrderMain::getUserId, userId);
        return orderMainDao.selectCount(wrapper);
    }
}
