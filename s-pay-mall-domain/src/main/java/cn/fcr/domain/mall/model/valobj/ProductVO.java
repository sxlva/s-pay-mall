package cn.fcr.domain.mall.model.valobj;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图对象，包含分类名称。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /** 分类名称 */
    private String category;

    /** 商品状态：0=下架，1=上架 */
    private Integer status;

    /** 创建时间 */
    @JsonProperty("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonProperty("update_time")
    private LocalDateTime updateTime;

    /** 分类名称（冗余字段） */
    @JsonProperty("category_name")
    private String categoryName;
}