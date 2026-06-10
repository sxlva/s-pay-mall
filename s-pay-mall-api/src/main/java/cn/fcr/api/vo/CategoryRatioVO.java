package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类销售占比视图对象
 */
@Data
public class CategoryRatioVO {

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("product_count")
    private Integer productCount;

    @JsonProperty("sales_amount")
    private BigDecimal salesAmount;
}
