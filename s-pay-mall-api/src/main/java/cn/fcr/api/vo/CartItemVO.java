package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车项视图对象
 *
 * @author 傅崇睿
 */
@Data
public class CartItemVO {

    /** 购物车项ID */
    private Long id;

    /** 商品ID */
    @JsonProperty("product_id")
    private Long productId;

    /** 商品名称 */
    @JsonProperty("product_name")
    private String productName;

    /** 商品单价 */
    @JsonProperty("product_price")
    private BigDecimal productPrice;

    /** 数量 */
    private Integer quantity;

    /** 是否选中 */
    private Boolean selected;

    /** 单项金额 */
    @JsonProperty("item_amount")
    private BigDecimal itemAmount;
}
