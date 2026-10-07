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

    /**
     * 保存订单实体
     * 新增或整单更新订单主记录
     *
     * @param orderEntity 订单实体
     */
    void saveOrder(OrderEntity orderEntity);

    /**
     * 条件查询订单列表
     * 用户端仅按 userId 过滤，管理端可组合状态与时间范围
     *
     * @param userId 用户ID
     * @param status 订单状态（可选）
     * @param start  下单时间起（可选，格式 YYYY-MM-DD）
     * @param end    下单时间止（可选，格式 YYYY-MM-DD）
     * @return 订单 VO 列表
     */
    List<OrderVO> findOrders(Long userId, String status, String start, String end);

    /**
     * 根据订单ID查询订单
     *
     * @param id 订单ID
     * @return 订单实体，不存在时返回 null
     */
    OrderEntity findById(Long id);

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单实体，不存在时返回 null
     */
    OrderEntity findByOrderNo(String orderNo);

    /**
     * 根据订单ID删除订单记录
     *
     * @param id 订单ID
     * @return 影响行数
     */
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