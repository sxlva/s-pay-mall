package cn.fcr.domain.mall.gateway;

import cn.fcr.domain.mall.model.valobj.OrderSummaryVO;

/**
 * 订单查询网关接口
 * 提供 mall 领域所需的订单只读查询操作
 * <p>
 * 【DDD 解耦】此接口定义在 mall 领域，作为对 order 领域的抽象依赖，
 * 由 Infrastructure 层实现，避免 mall 领域直接依赖 order 领域
 */
public interface IOrderQueryGateway {

    /**
     * 统计指定用户的订单数量
     * 用于删除用户前检查关联订单
     *
     * @param userId 用户ID
     * @return 订单数量
     */
    long countOrdersByUserId(Long userId);

    /**
     * 根据订单号查询支付订单摘要
     * <p>
     * 【上下文映射】返回 mall 领域的 OrderSummaryVO，由 Infrastructure 层
     * 将 order 领域的 PayOrderEntity 转换为 mall 领域关心的摘要信息。
     *
     * @param orderNo 订单号
     * @return 订单摘要，若不存在返回 null
     */
    OrderSummaryVO findPayOrderByOrderNo(String orderNo);
}
