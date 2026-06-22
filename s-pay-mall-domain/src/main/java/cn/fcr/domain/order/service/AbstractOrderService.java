package cn.fcr.domain.order.service;

import cn.fcr.domain.order.gateway.IPaymentGateway;
import cn.fcr.domain.order.gateway.IProductGateway;
import cn.fcr.domain.order.adapter.repository.IOrderRepository;
import cn.fcr.domain.order.model.aggregate.CreateOrderAggregate;
import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.entity.ProductEntity;
import cn.fcr.domain.order.model.entity.ShopCartEntity;
import cn.fcr.domain.order.model.valobj.OrderStatusVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 旧订单领域服务基类，提供订单创建与超时关单的抽象流程。
 *
 * @author 傅崇睿
 * @deprecated 当前订单创建主流程已迁移至 MallOrderServiceImpl
 */
@Slf4j
public abstract class AbstractOrderService implements IOrderService {

    /** 订单仓储 */
    protected final IOrderRepository repository;
    /** 商品网关 */
    protected final IProductGateway productGateway;
    /** 支付网关 */
    protected final IPaymentGateway paymentGateway;

    /**
     * 构造器注入领域依赖。
     *
     * @param repository 订单仓储
     * @param productGateway 商品网关
     * @param paymentGateway 支付网关
     */
    public AbstractOrderService(IOrderRepository repository, IProductGateway productGateway, IPaymentGateway paymentGateway) {
        this.repository = repository;
        this.productGateway = productGateway;
        this.paymentGateway = paymentGateway;
    }

    /**
     * 创建订单并生成支付单，含未支付订单复用逻辑。
     *
     * @param shopCartEntity 购物车实体
     * @return 支付订单实体，含订单号和支付链接
     * @throws Exception 支付预下单异常
     */
    @Override
    public PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception {
        OrderEntity unpaidOrderEntity = repository.queryUnPayOrder(shopCartEntity);
        if (null != unpaidOrderEntity && OrderStatusVO.PAY_WAIT.equals(unpaidOrderEntity.getOrderStatusVO())) {
            log.info("创建订单-存在，已存在未支付订单。userId:" + shopCartEntity.getUserId() + " productId:" + shopCartEntity.getProductId() + " orderId:" + unpaidOrderEntity.getOrderId());
            return PayOrderEntity.builder()
                    .orderNo(unpaidOrderEntity.getOrderId())
                    .payUrl(unpaidOrderEntity.getPayUrl())
                    .build();
        } else if (null != unpaidOrderEntity && OrderStatusVO.CREATE.equals(unpaidOrderEntity.getOrderStatusVO())) {
            log.info("创建订单-存在，存在未创建支付单订单，创建支付单开始 userId:" + shopCartEntity.getUserId() + " productId:" + shopCartEntity.getProductId() + " orderId:" + unpaidOrderEntity.getOrderId());
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
        log.info("创建订单-完成，生成支付单。userId: " + shopCartEntity.getUserId() + " orderId: " + orderEntity.getOrderId() + " payUrl: " + payOrderEntity.getPayUrl());

        return PayOrderEntity.builder()
                .orderNo(orderEntity.getOrderId())
                .payUrl(payOrderEntity.getPayUrl())
                .build();
    }

    /**
     * 处理超时关单：校验状态 → 状态机变更 → 持久化 → 恢复库存。
     *
     * @param orderNo 订单号
     * @return true 表示关单成功，false 表示无需处理或处理失败
     */
    @Override
    public boolean handleTimeoutCloseOrder(String orderNo) {
        log.info("处理超时关单: orderNo=" + orderNo);

        // 1. 查询订单
        OrderEntity order = repository.findByOrderNo(orderNo);
        if (order == null) {
            log.warn("订单不存在，可能已被删除: orderNo=" + orderNo);
            return false;
        }

        // 2. 【关键】判断当前订单状态是否允许"取消"（即"超时"语义校验）
        if (!order.canCancel()) {
            log.info("订单状态不允许取消: orderNo=" + orderNo + ", status=" + order.getSafeStateDesc());
            return false;
        }

        try {
            // 3. 状态机变更：调用 Entity 内部方法
            order.closeByTimeout();

            // 4.  持久化订单状态变更
            repository.save(order);

            // 5. 恢复库存
            List<Map<String, Object>> orderItems = repository.queryOrderItems(orderNo);
            for (Map<String, Object> item : orderItems) {
                String productId = (String) item.get("productId");
                Integer quantity = (Integer) item.get("quantity");
                productGateway.restoreStock(productId, quantity);
                log.info("已恢复库存: productId=" + productId + ", quantity=" + quantity);
            }

            log.info("超时关单成功: orderNo=" + orderNo);
            return true;
        } catch (IllegalStateException e) {
            // 状态守卫拒绝操作（并发修改导致状态已变更）
            log.warn("订单状态守卫拒绝关单: orderNo=" + orderNo + ", reason=" + e.getMessage());
            return false;
        }
    }

    /**
     * 持久化订单聚合根，由子类实现具体存储逻辑。
     *
     * @param orderAggregate 订单聚合根
     */
    protected abstract void doSaveOrder(CreateOrderAggregate orderAggregate);

    /**
     * 执行支付预下单，由子类对接具体支付渠道。
     *
     * @param userId 用户 ID
     * @param productId 商品 ID
     * @param productName 商品名称
     * @param orderId 订单号
     * @param totalAmount 支付金额
     * @return 支付订单实体
     * @throws Exception 支付预下单异常
     */
    protected abstract PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount) throws Exception;
}
