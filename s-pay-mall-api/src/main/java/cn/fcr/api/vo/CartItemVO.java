package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车项视图对象
 */
@Data
public class CartItemVO {
    private Long id;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_price")
    private BigDecimal productPrice;

    private Integer quantity;
    private Boolean selected;

    @JsonProperty("item_amount")
    private BigDecimal itemAmount;
}
