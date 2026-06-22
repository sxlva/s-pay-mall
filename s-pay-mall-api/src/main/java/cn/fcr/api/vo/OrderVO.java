package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象
 *
 * @author 傅崇睿
 */
@Data
public class OrderVO {

    /** 订单主键ID */
    private Long id;

    /** 订单号 */
    @JsonProperty("order_no")
    private String orderNo;

    /** 用户ID */
    @JsonProperty("user_id")
    private Long userId;

    /** 订单状态 */
    private String status;

    /** 订单状态描述 */
    @JsonProperty("status_desc")
    private String statusDesc;

    /** 订单总金额 */
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    /** 收货地址 */
    private String address;

    /** 创建时间 */
    @JsonProperty("create_time")
    private LocalDateTime createTime;
}
