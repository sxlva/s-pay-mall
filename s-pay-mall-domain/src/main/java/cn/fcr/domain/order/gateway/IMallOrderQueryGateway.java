package cn.fcr.domain.order.gateway;

import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.order.model.valobj.OrderVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商城订单查询网关，提供订单查询、统计等只读操作。
 *
 * @author 傅崇睿
 */
public interface IMallOrderQueryGateway {

    void saveOrder(OrderEntity orderEntity);

    List<OrderVO> findOrders(Long userId, String status, String start, String end);

    OrderEntity findById(Long id);

    OrderEntity findByOrderNo(String orderNo);

    int deleteById(Long id);

    int updateOrderStatus(Long orderId, String status);

    /**
     * 按订单号条件更新订单状态（乐观并发守卫）
     * UPDATE 带源状态条件（expectStatus，须为源状态的 {@link cn.fcr.domain.order.model.entity.OrderState#toDbStatus()} 存储形式），
     * 影响行数为 0 表示状态已被并发事务流转，调用方须据此跳过后续副作用（P0-9）
     *
     * @param orderNo      订单号
     * @param expectStatus 期望的源状态（数据库存储形式）
     * @param targetStatus 目标状态
     * @return 影响行数，0 表示守卫拒绝（并发/重复流转）
     */
    int updateOrderStatusByOrderNo(String orderNo, String expectStatus, String targetStatus);

    BigDecimal sumDailySales(String date);

    Integer countDailyOrders(String date);
}