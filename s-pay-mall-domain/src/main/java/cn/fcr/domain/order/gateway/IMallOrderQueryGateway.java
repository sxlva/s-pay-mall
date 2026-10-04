package cn.fcr.domain.order.gateway;

import cn.fcr.domain.order.model.entity.OrderEntity;
import cn.fcr.domain.order.model.valobj.OrderVO;

import java.util.List;

/**
 * 商城订单网关，提供订单保存、查询、删除与条件状态更新。
 * 日销/订单数统计由 mall/statistics 领域的 IStatisticsRepository 承担。
 *
 * @author 傅崇睿
 */
public interface IMallOrderQueryGateway {

    void saveOrder(OrderEntity orderEntity);

    List<OrderVO> findOrders(Long userId, String status, String start, String end);

    OrderEntity findById(Long id);

    OrderEntity findByOrderNo(String orderNo);

    int deleteById(Long id);

    /**
     * 按订单号条件更新订单状态（乐观并发守卫，P0-9）
     * UPDATE 带源状态条件，影响行数为 0 表示状态已被并发事务流转，调用方须据此跳过后续副作用。
     * 【状态口径】expectStatus 为源状态的 {@link cn.fcr.domain.order.model.entity.OrderState#toDbStatus()}
     * 存储形式（仅 INIT 为 CREATED，其余状态 code 与存储形式一致）；targetStatus 为目标状态的
     * {@link cn.fcr.domain.order.model.entity.OrderState#getCode()}（与 E2E 验收固化的存储值一致）
     *
     * @param orderNo      订单号
     * @param expectStatus 期望的源状态（数据库存储形式）
     * @param targetStatus 目标状态（领域 code 形式）
     * @return 影响行数，0 表示守卫拒绝（并发/重复流转）
     */
    int updateOrderStatusByOrderNo(String orderNo, String expectStatus, String targetStatus);
}