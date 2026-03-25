package cn.fcr.infrastructure.gateway.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author 傅崇睿
 * @date 2025/7/28 22:31
 * @description
 */
@Data
public class ProductDTO {

    /** 商品ID */
    private String productId;
    /** 商品名称 */
    private String productName;
    /** 商品描述 */
    private String productDesc;
    /** 商品价格 */
    private BigDecimal price;

}
