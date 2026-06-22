package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类销售占比视图对象
 *
 * @author 傅崇睿
 */
@Data
public class CategoryRatioVO {

    /** 分类名称 */
    @JsonProperty("category_name")
    private String categoryName;

    /** 商品数量 */
    @JsonProperty("product_count")
    private Integer productCount;

    /** 销售额 */
    @JsonProperty("sales_amount")
    private BigDecimal salesAmount;
}
