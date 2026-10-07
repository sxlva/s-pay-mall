package cn.fcr.api.dto.admin.res;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象
 *
 * @author 傅崇睿
 */
@Data
public class AdminOrderRes {

    /** 订单主键ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 订单状态 */
    private String status;

    /** 订单状态描述 */
    private String statusDesc;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 收货地址 */
    private String address;

    /** 创建时间 */
    private LocalDateTime createTime;
}
