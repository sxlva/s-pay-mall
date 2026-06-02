package cn.fcr.domain.mall.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
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

    public boolean canPay() {
        return this.state == OrderState.INIT;
    }

    public boolean canCancel() {
        return this.state == OrderState.INIT;
    }

    public boolean canDeliver() {
        return this.state == OrderState.PAID;
    }

    public boolean canComplete() {
        return this.state == OrderState.SHIPPED;
    }

    public void pay() {
        if (!canPay()) {
            throw new IllegalStateException(
                "订单当前状态为 [" + (this.state != null ? this.state.getDescription() : "null") +
                "]，无法执行支付操作，仅 [待支付] 状态的订单可以支付"
            );
        }
        this.state = OrderState.PAID;
        this.updateTime = LocalDateTime.now();
    }

    public void cancel() {
        if (!canCancel()) {
            throw new IllegalStateException(
                "订单当前状态为 [" + (this.state != null ? this.state.getDescription() : "null") +
                "]，无法执行取消操作，仅 [待支付] 状态的订单可以取消"
            );
        }
        this.state = OrderState.CANCELED;
        this.updateTime = LocalDateTime.now();
    }

    public void deliver() {
        if (!canDeliver()) {
            throw new IllegalStateException(
                "订单当前状态为 [" + (this.state != null ? this.state.getDescription() : "null") +
                "]，无法执行发货操作，仅 [已支付] 状态的订单可以发货"
            );
        }
        this.state = OrderState.SHIPPED;
        this.updateTime = LocalDateTime.now();
    }

    public void complete() {
        if (!canComplete()) {
            throw new IllegalStateException(
                "订单当前状态为 [" + (this.state != null ? this.state.getDescription() : "null") +
                "]，无法执行完成操作，仅 [已发货] 状态的订单可以完成"
            );
        }
        this.state = OrderState.DONE;
        this.updateTime = LocalDateTime.now();
    }

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
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }

    public void clearItems() {
        if (this.items != null) {
            this.items.clear();
        }
    }
}