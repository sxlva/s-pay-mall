package cn.fcr.domain.order.service;

import cn.fcr.domain.order.gateway.IPaymentGateway;
import cn.fcr.domain.order.gateway.IProductGateway;
import cn.fcr.domain.order.adapter.repository.IOrderRepository;
import cn.fcr.domain.order.model.aggregate.CreateOrderAggregate;
import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.entity.ProductEntity;
import cn.fcr.domain.order.model.entity.ShopCartEntity;
import cn.fcr.domain.order.model.valobj.OrderStatusVO;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractOrderService implements IOrderService {

    protected final IOrderRepository repository;
    protected final IProductGateway productGateway;
    protected final IPaymentGateway paymentGateway;

    public AbstractOrderService(IOrderRepository repository, IProductGateway productGateway, IPaymentGateway paymentGateway) {
        this.repository = repository;
        this.productGateway = productGateway;
        this.paymentGateway = paymentGateway;
    }

    @Override
    public PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception {
        OrderEntity unpaidOrderEntity = repository.queryUnPayOrder(shopCartEntity);
        if (null != unpaidOrderEntity && OrderStatusVO.PAY_WAIT.equals(unpaidOrderEntity.getOrderStatusVO())) {
            log.info("创建订单-存在，已存在未支付订单。userId:{} productId:{} orderId:{}", shopCartEntity.getUserId(), shopCartEntity.getProductId(), unpaidOrderEntity.getOrderId());
            return PayOrderEntity.builder()
                    .orderNo(unpaidOrderEntity.getOrderId())
                    .payUrl(unpaidOrderEntity.getPayUrl())
                    .build();
        } else if (null != unpaidOrderEntity && OrderStatusVO.CREATE.equals(unpaidOrderEntity.getOrderStatusVO())) {
            log.info("创建订单-存在，存在未创建支付单订单，创建支付单开始 userId:{} productId:{} orderId:{}", shopCartEntity.getUserId(), shopCartEntity.getProductId(), unpaidOrderEntity.getOrderId());
            PayOrderEntity payOrderEntity = doPrepayOrder(shopCartEntity.getUserId(), shopCartEntity.getProductId(), unpaidOrderEntity.getProductName(), unpaidOrderEntity.getOrderId(), unpaidOrderEntity.getTotalAmount());
            return PayOrderEntity.builder()
                    .orderNo(payOrderEntity.getOrderNo())
                    .payUrl(payOrderEntity.getPayUrl())
                    .build();
        }

        ProductEntity productEntity = productGateway.queryProductByProductId(shopCartEntity.getProductId());
        OrderEntity orderEntity = CreateOrderAggregate.buildOrderEntity(productEntity.getProductId(), productEntity.getProductName());
        CreateOrderAggregate orderAggregate = CreateOrderAggregate.builder()
                .userId(shopCartEntity.getUserId())
                .productEntity(productEntity)
                .orderEntity(orderEntity)
                .build();

        this.doSaveOrder(orderAggregate);

        PayOrderEntity payOrderEntity = doPrepayOrder(shopCartEntity.getUserId(), productEntity.getProductId(), productEntity.getProductName(), orderEntity.getOrderId(), productEntity.getPrice());
        log.info("创建订单-完成，生成支付单。userId: {} orderId: {} payUrl: {}", shopCartEntity.getUserId(), orderEntity.getOrderId(), payOrderEntity.getPayUrl());

        return PayOrderEntity.builder()
                .orderNo(orderEntity.getOrderId())
                .payUrl(payOrderEntity.getPayUrl())
                .build();
    }

    @Override
    public boolean handleTimeoutCloseOrder(String orderNo) {
        log.info("处理超时关单: orderNo={}", orderNo);

        String currentStatus = repository.queryOrderStatus(orderNo);
        if (currentStatus == null) {
            log.warn("订单不存在，可能已被删除: orderNo={}", orderNo);
            return false;
        }

        if (!OrderStatusVO.CREATE.getCode().equals(currentStatus)) {
            log.info("订单状态已变更，无需关单: orderNo={}, status={}", orderNo, currentStatus);
            return false;
        }

        boolean closed = repository.closeOrderWithOptimisticLock(orderNo, OrderStatusVO.CREATE.getCode());
        if (!closed) {
            log.info("订单状态已被其他线程修改，关单失败: orderNo={}", orderNo);
            return false;
        }

        List<Map<String, Object>> orderItems = repository.queryOrderItems(orderNo);
        for (Map<String, Object> item : orderItems) {
            String productId = (String) item.get("productId");
            Integer quantity = (Integer) item.get("quantity");
            productGateway.restoreStock(productId, quantity);
            log.info("已恢复库存: productId={}, quantity={}", productId, quantity);
        }

        log.info("超时关单成功: orderNo={}", orderNo);
        return true;
    }

    protected abstract void doSaveOrder(CreateOrderAggregate orderAggregate);

    protected abstract PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount) throws Exception;
}