package cn.fcr.domain.mall.model.entity;

import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.valobj.PayStatus;
import cn.fcr.types.util.DateUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter; // 替换 @Data，拒绝外界盲目 Setter
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections; // 引入只读防御
import java.util.List;
import java.util.UUID;

@Getter // 仅允许外部读取
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderEntity {

    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private String address;
    private OrderState state;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();

    /* ==========================================
     * 状态机守卫变迁逻辑 (防腐状态控制)
     * ========================================== */
    public boolean canPay() { return this.state == OrderState.INIT; }
    public boolean canCancel() { return this.state == OrderState.INIT; }
    public boolean canDeliver() { return this.state == OrderState.PAID; }
    public boolean canComplete() { return this.state == OrderState.SHIPPED; }

    public void pay() {
        if (!canPay()) {
            throw new IllegalStateException("订单状态为 [" + getSafeStateDesc() + "]，拒绝支付操作");
        }
        this.state = OrderState.PAID;
        this.updateTime = DateUtils.now();
    }

    public void cancel() {
        if (!canCancel()) {
            throw new IllegalStateException("订单状态为 [" + getSafeStateDesc() + "]，拒绝取消操作");
        }
        this.state = OrderState.CANCELED;
        this.updateTime = DateUtils.now();
    }

    public void deliver() {
        if (!canDeliver()) {
            throw new IllegalStateException("订单状态为 [" + getSafeStateDesc() + "]，拒绝发货操作");
        }
        this.state = OrderState.SHIPPED;
        this.updateTime = DateUtils.now();
    }

    public void complete() {
        if (!canComplete()) {
            throw new IllegalStateException("订单状态为 [" + getSafeStateDesc() + "]，拒绝完成操作");
        }
        this.state = OrderState.DONE;
        this.updateTime = DateUtils.now();
    }

    private String getSafeStateDesc() {
        return this.state != null ? this.state.getDescription() : "null";
    }

    /* ==========================================
     * 高内聚行为与安全集合封装
     * ========================================== */
    public BigDecimal calculateTotalAmount() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(OrderItemEntity::calculateItemAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addItem(OrderItemEntity item) {
        if (item == null) {
            throw new IllegalArgumentException("订单项不能为空");
        }
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        // 级联刷新总金额，确保对象内存状态的一致性
        this.totalAmount = calculateTotalAmount();
    }

    public void clearItems() {
        this.items = new ArrayList<>();
        this.totalAmount = BigDecimal.ZERO;
        this.updateTime = DateUtils.now();
    }

    /**
     * 核心安全防御：返回只读集合视图，外部调用 .add() / .clear() 将直接抛出 UnsupportedOperationException
     */
    public List<OrderItemEntity> getItems() {
        if (this.items == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.items);
    }

    /**
     * 允许外部在有限的业务场景下修改收货地址
     */
    public void changeAddress(String newAddress) {
        if (newAddress == null || newAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("收货地址不能为空");
        }
        if (this.state != OrderState.INIT) {
            throw new IllegalStateException("订单已进入后续流程，无法修改地址");
        }
        this.address = newAddress;
        this.updateTime = DateUtils.now();
    }

    /* ==========================================
     * 领域静态工厂方法
     * ========================================== */
    public static OrderEntity createFromCart(Long userId, String address, List<CartItemVO> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("购物车数据流为空，无法组装订单");
        }

        String orderNo = "ORD" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 29).toUpperCase();

        OrderEntity order = OrderEntity.builder()
                .orderNo(orderNo)
                .userId(userId)
                .address(address)
                .state(OrderState.INIT)
                .createTime(DateUtils.now())
                .updateTime(DateUtils.now())
                .items(new ArrayList<>())
                .build();

        for (CartItemVO cartItem : cartItems) {
            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .price(cartItem.getProductPrice())
                    .quantity(cartItem.getQuantity())
                    .createTime(DateUtils.now())
                    .build();
            order.addItem(orderItem); // 内部已自动内聚金额计算逻辑
        }

        return order;
    }

    /* ==========================================
     * 跨上下文对象映射 (Context Mapper)
     * ========================================== */
    public PayOrderEntity toPayOrder() {
        if (this.items == null || this.items.isEmpty()) {
            throw new IllegalStateException("当前商城订单无子项，拒绝生成支付上下文");
        }

        StringBuilder productNameBuilder = new StringBuilder();
        StringBuilder productIds = new StringBuilder();

        for (int i = 0; i < this.items.size(); i++) {
            OrderItemEntity item = this.items.get(i);
            if (i > 0) {
                productNameBuilder.append("、");
                productIds.append(",");
            }
            productNameBuilder.append(item.getProductName());
            productIds.append(item.getProductId());
        }

        return PayOrderEntity.builder()
                .orderNo(this.orderNo)
                .userId(String.valueOf(this.userId))
                .productId(productIds.toString())
                .productName(productNameBuilder.toString())
                .totalAmount(this.totalAmount)
                .status(PayStatus.WAIT_PAY)
                .createTime(DateUtils.now())
                .updateTime(DateUtils.now())
                .build();
    }
}