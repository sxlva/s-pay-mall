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

    /**
     * 根据订单号查询完整订单（聚合根查询）
     * 【DDD】用于获取完整的 OrderEntity 对象以进行业务操作
     *
     * @param orderNo 订单号
     * @return 订单实体，若不存在返回 null
     */
    OrderEntity findByOrderNo(String orderNo);

    /**
     * 保存订单聚合根
     * 【DDD】聚合根的状态变更由 Entity 内部处理，Repository 仅负责持久化
     *
     * @param order 订单实体
     */
    void save(OrderEntity order);

    /**
     * 统计指定用户的订单数量
     * 用于删除用户前检查关联订单
     *
     * @param userId 用户ID
     * @return 订单数量
     */
    long countByUserId(Long userId);
}
