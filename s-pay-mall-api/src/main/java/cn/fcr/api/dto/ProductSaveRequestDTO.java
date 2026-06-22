package cn.fcr.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品保存请求DTO
 * <p>
 * 用于创建或更新商品的请求参数封装
 *
 * @author 傅崇睿
 */
@Data
public class ProductSaveRequestDTO {

    /**
     * 商品ID，创建时为null，更新时必填
     */
    private Long id;

    /**
     * 商品分类ID，前端使用snake_case命名
     */
    @NotNull(message = "商品分类ID不能为空")
    private Long categoryId;

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    private String name;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品价格
     */
    @NotNull(message = "商品价格不能为空")
    private BigDecimal price;

    /**
     * 商品库存数量
     */
    @NotNull(message = "商品库存数量不能为空")
    private Integer stock;

    /**
     * 商品状态：0-下架，1-上架
     */
    @NotNull(message = "商品状态不能为空")
    private Integer status;
}
