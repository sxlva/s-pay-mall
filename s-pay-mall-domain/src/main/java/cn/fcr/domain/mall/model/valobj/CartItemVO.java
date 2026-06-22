package cn.fcr.domain.mall.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 购物车商品项视图对象，承载前端展示所需的聚合数据。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItemVO {

    /** 购物车项ID */
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品单价 */
    private BigDecimal productPrice;

    /** 购买数量 */
    private Integer quantity;

    /** 是否选中 */
    private Boolean selected;

    /** 该项总金额 */
    private BigDecimal itemAmount;

    /** 当前库存 */
    private Integer stock;

}
