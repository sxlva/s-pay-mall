package cn.fcr.api.dto.admin.res;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售趋势视图对象
 *
 * @author 傅崇睿
 */
@Data
public class AdminSalesTrendRes {

    /** 日期 */
    private String date;

    /** 销售额 */
    private BigDecimal salesAmount;

    /** 订单数 */
    private Integer orderCount;
}
