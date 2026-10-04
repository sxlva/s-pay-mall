package cn.fcr.domain.order.service;

/**
 * 订单状态机服务接口，统一处理 order_main 和 pay_order 的状态流转，
 * 确保数据一致性，封装所有状态转换的业务规则和约束。
 *
 * @author 傅崇睿
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
     * 取消待支付订单
     * 仅允许 INIT（待支付）状态取消（设计文档 module-order-pay.md §四：PAY_WAIT --> CLOSE），
     * 仅完成 DB 状态流转：order_main 更新为 CANCELED；若 pay_order 仍为 WAIT_PAY/PAYING 则关闭为 CLOSED。
     * 库存恢复不在本方法内执行，须由调用方在事务提交后调用
     * {@link #restoreStockForCancel(String)}（P1-2：避免 DB 回滚时 Redis 无法回滚）。
     *
     * @param orderNo 订单号
     * @return 是否更新成功（false = 订单不存在或状态不允许取消，或已被并发流转）
     */
    boolean cancel(String orderNo);

    /**
     * 取消订单后恢复 Redis 预扣库存（须在取消事务提交后调用）
     *
     * <p>cancel 守卫仅允许 INIT（待支付）状态流转，此时尚未同步扣减 MySQL 库存，故仅恢复 Redis。
     * 注意：重复调用会重复加回库存，调用方必须仅在 cancel 返回 true 后调用一次，
     * 不可在 MQ 重试等场景下无条件重放。</p>
     *
     * @param orderNo 订单号
     */
    void restoreStockForCancel(String orderNo);
}