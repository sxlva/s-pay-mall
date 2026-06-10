package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建结果视图对象
 */
@Data
public class OrderCreateVO {

    @JsonProperty("order_no")
    private String orderNo;

    @JsonProperty("pay_url")
    private String payUrl;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
}
