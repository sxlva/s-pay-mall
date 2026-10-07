package cn.fcr.domain.order.gateway;

import cn.fcr.domain.order.model.vo.PayStatus;

import java.util.List;

/**
 * 支付订单网关接口，提供支付订单状态更新的能力。
 *
 * @author 傅崇睿
 */
public interface IPayOrderGateway {

    /**
     * 更新支付订单状态为已支付
     *
     * @param orderNo 订单号
     * @return 是否更新成功
     */
    boolean updatePayStatusToPaid(String orderNo);

    /**
     * 更新支付订单状态为交易完成
     *
     * @param orderNo 订单号
     * @return 是否更新成功
     */
    boolean updatePayStatusToTradeDone(String orderNo);

    /**
     * 关闭支付订单
     *
     * @param orderNo 订单号
     * @return 是否关闭成功
     */
    boolean closePayOrder(String orderNo);

    /**
     * 获取支付订单当前状态
     *
     * @param orderNo 订单号
     * @return 支付状态，若不存在返回 null
     */
    PayStatus getPayStatus(String orderNo);

    /**
     * 查询等待支付超过5分钟但未收到回调的订单号
     * 用于 NoPayNotifyOrderJob 主动补单（数据口径：pay_order 表 WAIT_PAY 行）
     *
     * @return 需要主动补单的订单号列表
     */
    List<String> queryNoPayNotifyOrder();

    /**
     * 查询等待支付超过40分钟仍未关闭的订单号（pay_order 表 WAIT_PAY 行）
     * 用于 NoPayNotifyOrderJob 兜底关单补偿（TD-7）：仅兜住 30 分钟延时关单消息
     * 丢失/消费失败的漏网订单，以 40 分钟为界避免与正常关单链路竞争
     *
     * @return 需要兜底关单的订单号列表
     */
    List<String> queryStaleWaitPayOrders();
}