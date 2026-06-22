package cn.fcr.domain.mall.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单商品项实体，管理单个订单项的商品信息、价格和数量。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemEntity {

    /** 订单项ID */
    private Long id;
    /** 订单ID */
    private Long orderId;
    /** 商品ID */
    private Long productId;
    /** 商品名称（快照） */
    private String productName;
    /** 商品单价（快照） */
    private BigDecimal price;
    /** 购买数量 */
    private Integer quantity;
    /** 创建时间 */
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