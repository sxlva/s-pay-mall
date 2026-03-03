package cn.fcr.domain.order.adapter.repository;

import cn.fcr.domain.order.model.aggregate.CreateOrderAggregate;
import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.entity.ShopCartEntity;

import java.util.List;

/**
 * @author xiaolv
 * @date 2025/7/28 19:31
 * @description
 */
public interface IOrderRepository {

    public OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity);

    public void doSaveOrder(CreateOrderAggregate orderAggregate);

    void updateOrderPayInfo(PayOrderEntity payOrderEntity);

    void changeOrderPaySuccess(String orderId);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId);
}
