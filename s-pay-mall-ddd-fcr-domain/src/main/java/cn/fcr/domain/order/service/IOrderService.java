package cn.fcr.domain.order.service;

import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.entity.ShopCartEntity;

import java.util.List;

/**
 * @author xiaolv
 * @date 2025/7/28 19:27
 * @description 订单接口
 */
public interface IOrderService {

    PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception;

    void changeOrderPaySuccess(String orderId);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId);

}
