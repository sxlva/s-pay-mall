package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售趋势视图对象
 */
@Data
public class SalesTrendVO {
    private String date;

    @JsonProperty("sales_amount")
    private BigDecimal salesAmount;

    @JsonProperty("order_count")
    private Integer orderCount;
}
