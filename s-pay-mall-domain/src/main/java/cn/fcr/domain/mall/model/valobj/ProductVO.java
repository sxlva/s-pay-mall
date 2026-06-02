package cn.fcr.domain.mall.model.valobj;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图对象（包含分类名称）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO {

    private Long id;

    @JsonProperty("category_id")
    private Long categoryId;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private String category;

    private Integer status;

    @JsonProperty("create_time")
    private LocalDateTime createTime;

    @JsonProperty("update_time")
    private LocalDateTime updateTime;

    @JsonProperty("category_name")
    private String categoryName;
}