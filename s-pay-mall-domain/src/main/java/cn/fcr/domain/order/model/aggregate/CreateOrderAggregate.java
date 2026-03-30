package cn.fcr.domain.order.model.aggregate;

import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.order.model.entity.ProductEntity;
import cn.fcr.domain.order.model.valobj.OrderStatusVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;

/**
 * @author 傅崇睿
 * @date 2025/7/28 21:35
 * @description 订单聚合对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderAggregate {

    private String userId;

    private ProductEntity productEntity;

    private OrderEntity orderEntity;

    public static OrderEntity buildOrderEntity(String productId, String productName){
        return OrderEntity.builder()
                .productId(productId)
                .productName(productName)
                .orderId(RandomStringUtils.randomNumeric(14))
                .orderTime(new Date())
                .orderStatusVO(OrderStatusVO.CREATE)
                .build();
    }

}
