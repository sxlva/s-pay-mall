package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售趋势视图对象
 *
 * @author 傅崇睿
 */
@Data
public class SalesTrendVO {

    /** 日期 */
    private String date;

    /** 销售额 */
    @JsonProperty("sales_amount")
    private BigDecimal salesAmount;

    /** 订单数 */
    @JsonProperty("order_count")
    private Integer orderCount;
}
