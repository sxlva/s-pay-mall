package cn.fcr.domain.mall.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemEntity {

    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private LocalDateTime createTime;

    public BigDecimal calculateItemAmount() {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public void addQuantity(Integer delta) {
        if (delta == null || delta <= 0) {
            return;
        }
        int newQuantity = (this.quantity == null ? 0 : this.quantity) + delta;
        if (newQuantity > 999) {
            throw new IllegalArgumentException("订单项商品数量不能超过999");
        }
        this.quantity = newQuantity;
    }
}