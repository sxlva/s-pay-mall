package cn.fcr.domain.mall.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单创建结果值对象，包含支付链接等下单响应信息。
 *
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateVO {

    /** 订单号 */
    private String orderNo;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 订单状态码 */
    private String status;

    /** 支付链接URL */
    private String payUrl;
}
