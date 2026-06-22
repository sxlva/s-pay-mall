package cn.fcr.domain.mall.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车商品项实体，管理单个商品在购物车中的数量、价格和选中状态。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItemEntity {

    /** 购物车项ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 商品名称（快照） */
    private String productName;

    /** 商品单价（快照） */
    private BigDecimal productPrice;

    /** 购买数量 */
    private Integer quantity;

    /** 是否选中 */
    private Boolean selected;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
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
