package cn.fcr.domain.mall.service;

/**
 * 订单状态机服务接口
 * 统一处理 order_main 和 pay_order 的状态流转，确保数据一致性
 * 
 * 【DDD 原则】状态机作为领域服务，封装所有状态转换的业务规则和约束
 */
public interface IOrderStateMachineService {

    /**
     * 订单支付成功
     * 同时更新 order_main 为 PAID，pay_order 为 PAID
     *
     * @param orderNo 订单号
     * @return 是否更新成功
     */
    boolean paySuccess(String orderNo);

    /**
     * 订单发货
     * 同时更新 order_main 为 SHIPPED，pay_order 为 TRADE_DONE
     *
     * @param orderNo 订单号
     * @return 是否更新成功
     */
    boolean deliver(String orderNo);

    /**
     * 订单完成
     * 更新 order_main 为 DONE
     *
     * @param orderNo 订单号
     * @return 是否更新成功
     */
    boolean complete(String orderNo);

    /**
     * 取消订单
     * 同时更新 order_main 为 CANCELED，pay_order 为 CLOSED（如果未支付）
     *
     * @param orderNo 订单号
     * @return 是否更新成功
     */
    boolean cancel(String orderNo);
}