package cn.bugstack.domain.order.service;

import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;

/**
 * @author xiaolv
 * @date 2025/7/28 19:27
 * @description 订单接口
 */
public interface IOrderService {

    PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception;

}
