package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图对象
 */
@Data
public class ProductVO {
    private Long id;

    @JsonProperty("category_id")
    private Long categoryId;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private String category;

    @JsonProperty("category_name")
    private String categoryName;

    private Integer status;

    @JsonProperty("create_time")
    private LocalDateTime createTime;
}
