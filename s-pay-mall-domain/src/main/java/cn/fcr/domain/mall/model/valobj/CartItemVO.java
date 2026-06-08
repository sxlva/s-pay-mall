package cn.fcr.domain.mall.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItemVO {

    private Long id;

    private Long productId;

    private String productName;

    private BigDecimal productPrice;

    private Integer quantity;

    private Boolean selected;

    private BigDecimal itemAmount;

    private Integer stock;

}
