package cn.bugstack.domain.order.model.entity;

import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author xiaolv
 * @date 2025/7/28 19:43
 * @description 完整订单
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderEntity {

    private String productId;
    private String productName;
    private String orderId;
    private Date orderTime;
    private BigDecimal totalAmount;
    private OrderStatusVO orderStatusVO; // 将原来的String字符串换成枚举类型
    private String payUrl;

}
