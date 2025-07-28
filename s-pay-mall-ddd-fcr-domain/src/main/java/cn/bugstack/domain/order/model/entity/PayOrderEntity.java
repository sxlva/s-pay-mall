package cn.bugstack.domain.order.model.entity;

import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaolv
 * @date 2025/7/21 07:13
 * @description 订单创建后返回给支付模块的响应对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayOrderEntity {

    private String userId;
    private String orderId;
    private String payUrl; // 支付的链
    private OrderStatusVO orderStatus; // order状态枚举

}
