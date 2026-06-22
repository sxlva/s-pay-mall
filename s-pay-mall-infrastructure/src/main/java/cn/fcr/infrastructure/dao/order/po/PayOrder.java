package cn.fcr.infrastructure.dao.order.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付订单实体类（pay_order表）
 *
 * @author 傅崇睿
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayOrder {
    /** 主键ID */
    private Long id;
    /** 用户ID */
    private String userId;
    /** 商品ID */
    private String productId;
    /** 商品名称 */
    private String productName;
    /** 订单号 */
    private String orderId;
    /** 下单时间 */
    private Date orderTime;
    /** 订单总金额 */
    private BigDecimal totalAmount;
    /** 订单状态 */
    private String status;
    /** 支付链接 */
    private String payUrl;
    /** 支付时间 */
    private Date payTime;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
