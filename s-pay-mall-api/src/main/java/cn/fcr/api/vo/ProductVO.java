package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图对象
 *
 * @author 傅崇睿
 */
@Data
public class ProductVO {

    /** 商品ID */
    private Long id;

    /** 分类ID */
    @JsonProperty("category_id")
    private Long categoryId;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 商品价格 */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 分类标识 */
    private String category;

    /** 分类名称 */
    @JsonProperty("category_name")
    private String categoryName;

    /** 商品状态：0-下架，1-上架 */
    private Integer status;

    /** 创建时间 */
    @JsonProperty("create_time")
    private LocalDateTime createTime;
}
