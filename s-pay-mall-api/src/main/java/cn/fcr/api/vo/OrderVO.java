package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象
 */
@Data
public class OrderVO {
    private Long id;

    @JsonProperty("order_no")
    private String orderNo;

    @JsonProperty("user_id")
    private Long userId;

    private String status;

    @JsonProperty("status_desc")
    private String statusDesc;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    private String address;

    @JsonProperty("create_time")
    private LocalDateTime createTime;
}
