package cn.fcr.infrastructure.order.gateway.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品数据传输对象（旧Order域使用）
 *
 * @author 傅崇睿
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
