package cn.fcr.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品保存请求DTO
 * <p>
 * 用于创建或更新商品的请求参数封装
 */
@Data
public class ProductSaveRequest {

    /**
     * 商品ID，创建时为null，更新时必填
     */
    private Long id;

    /**
     * 商品分类ID，前端使用snake_case命名
     */
    @JsonProperty("category_id")
    private Long categoryId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品库存数量
     */
    private Integer stock;

    /**
     * 商品状态：0-下架，1-上架
     */
    private Integer status;
}
