package cn.fcr.infrastructure.gateway.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @description 商品DTO
 * 
 * 【职责说明】
 * - 封装商品信息的数据传输对象
 * - 用于商品查询接口的数据传递
 * 
 * 【字段说明】
 * - productId: 商品ID
 * - productName: 商品名称
 * - productDesc: 商品描述
 * - price: 商品价格
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
