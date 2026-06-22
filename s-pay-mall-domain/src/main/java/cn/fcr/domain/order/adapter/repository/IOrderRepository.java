package cn.fcr.domain.order.adapter.repository;

import cn.fcr.domain.order.model.aggregate.CreateOrderAggregate;
import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.entity.ShopCartEntity;

import java.util.List;
import java.util.Map;

/**
 * 订单仓储接口，定义订单持久化操作的抽象
 *
 * @author 傅崇睿
 */
public interface IOrderRepository {

    /**
     * 查询未支付订单
     *
     * @param shopCartEntity 购物车实体
     * @return 订单实体
     */
    OrderEntity queryUnPayOrder(ShopCartEntity shopCartEntity);

    /**
     * 保存订单聚合根
     *
     * @param orderAggregate 订单创建聚合
     */
    void doSaveOrder(CreateOrderAggregate orderAggregate);

    /**
     * 更新订单支付信息
     *
     * @param payOrderEntity 支付订单实体
     */
    void updateOrderPayInfo(PayOrderEntity payOrderEntity);

    /**
     * 变更订单为支付成功
     *
     * @param orderId 订单ID
     */
    void changeOrderPaySuccess(String orderId);

    /**
     * 查询未收到支付回调通知的订单ID列表
     *
     * @return 订单ID列表
     */
    List<String> queryNoPayNotifyOrder();

    /**
     * 查询待超时关闭的订单ID列表
     *
     * @return 订单ID列表
     */
    List<String> queryTimeoutCloseOrderList();

    /**
     * 关闭订单
     *
     * @param orderId 订单ID
     * @return 是否成功关闭
     */
    boolean changeOrderClose(String orderId);

    /**
     * 查询订单状态
     *
     * @param orderNo 订单号
     * @return 订单状态码，若订单不存在返回 null
     */
    String queryOrderStatus(String orderNo);

    /**
     * 查询订单详情，包含商品信息
     *
     * @param orderNo 订单号
     * @return 订单详情映射列表，包含产品ID和数量
     */
    List<Map<String, Object>> queryOrderItems(String orderNo);

    /**
     * 乐观锁方式关闭订单
     *
     * @param orderNo      订单号
     * @param expectStatus 当前期望状态
     * @return 是否成功关闭
     */
    boolean closeOrderWithOptimisticLock(String orderNo, String expectStatus);

    /**
     * 根据订单号查询完整订单实体（聚合根查询）
     *
     * @param orderNo 订单号
     * @return 订单实体，若不存在返回 null
     */
    OrderEntity findByOrderNo(String orderNo);

    /**
     * 保存订单实体，聚合根状态变更由 Entity 内部处理，Repository 仅负责持久化
     *
     * @param order 订单实体
     */
    void save(OrderEntity order);

    /**
     * 统计指定用户的订单数量，用于删除用户前检查关联订单
     *
     * @param userId 用户ID
     * @return 订单数量
     */
    long countByUserId(Long userId);
}
