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
 * 订单创建聚合根
 *
 * @author 傅崇睿
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderAggregate {

    /** 用户ID */
    private String userId;

    /** 商品实体，本次下单的商品快照 */
    private ProductEntity productEntity;

    /** 订单实体 */
    private OrderEntity orderEntity;

    /**
     * 构建订单实体
     *
     * @param productId   商品ID
     * @param productName 商品名称
     * @return 新建的订单实体，状态为 CREATE
     */
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
