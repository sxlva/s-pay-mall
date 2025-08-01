package cn.bugstack.domain.order.adapter.repository;

import cn.bugstack.domain.order.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;

/**
 * @author xiaolv
 * @date 2025/7/28 19:31
 * @description
 */
public interface IOrderRepository {

    public OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity);

    public void doSaveOrder(CreateOrderAggregate orderAggregate);

    void updateOrderPayInfo(PayOrderEntity payOrderEntity);
}
