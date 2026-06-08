package cn.fcr.domain.order.adapter.repository;

import cn.fcr.domain.order.model.aggregate.CreateOrderAggregate;
import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.entity.ShopCartEntity;

import java.util.List;
import java.util.Map;

/**
 * @author 傅崇睿
 * @date 2025/7/28 19:31
 * @description 订单仓储接口
 */
public interface IOrderRepository {

    OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity);

    void doSaveOrder(CreateOrderAggregate orderAggregate);

    void updateOrderPayInfo(PayOrderEntity payOrderEntity);

    void changeOrderPaySuccess(String orderId);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId);

    /**
     * 查询订单状态
     *
     * @param orderNo 订单号
     * @return 订单状态，如果订单不存在返回null
     */
    String queryOrderStatus(String orderNo);

    /**
     * 查询订单详情，包含商品信息
     *
     * @param orderNo 订单号
     * @return 订单详情映射，包含产品ID和数量
     */
    List<Map<String, Object>> queryOrderItems(String orderNo);

    /**
     * 乐观锁方式关闭订单
     *
     * @param orderNo     订单号
     * @param expectStatus 期望状态
     * @return 是否成功关闭
     */
    boolean closeOrderWithOptimisticLock(String orderNo, String expectStatus);
}
