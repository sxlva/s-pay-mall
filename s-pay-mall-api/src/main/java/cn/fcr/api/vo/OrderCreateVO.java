package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建结果视图对象
 *
 * @author 傅崇睿
 */
@Data
public class OrderCreateVO {

    /** 订单号 */
    @JsonProperty("order_no")
    private String orderNo;

    /** 支付链接 */
    @JsonProperty("pay_url")
    private String payUrl;

    /** 订单总金额 */
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
}
