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
public class CartItemEntity {

    private Long id;

    private Long userId;

    private Long productId;

    private String productName;

    private BigDecimal productPrice;

    private Integer quantity;

    private Boolean selected;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public BigDecimal calculateItemAmount() {
        if (productPrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return productPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public void addQuantity(Integer delta) {
        if (delta == null) return;
        int newQuantity = (this.quantity == null ? 0 : this.quantity) + delta;
        if (newQuantity < 1) {
            throw new IllegalArgumentException("购物车商品数量不能小于1");
        }
        if (newQuantity > 999) {
            throw new IllegalArgumentException("购物车单个商品数量不能超过999");
        }
        this.quantity = newQuantity;
        this.updateTime = LocalDateTime.now();
    }

    public void toggleSelect() {
        this.selected = this.selected == null ? true : !this.selected;
    }

}
