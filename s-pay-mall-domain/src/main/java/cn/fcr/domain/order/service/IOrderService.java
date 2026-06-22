package cn.fcr.domain.order.service;

import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.entity.ShopCartEntity;

import java.util.List;

/**
 * 订单领域服务接口
 *
 * @author 傅崇睿
 */
public interface IOrderService {

    /**
     * 创建订单
     *
     * @param shopCartEntity 购物车实体
     * @return 支付订单实体
     * @throws Exception 创建过程中可能抛出异常
     */
    PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception;

    /**
     * 变更订单为支付成功状态
     *
     * @param orderId 订单ID
     */
    void changeOrderPaySuccess(String orderId);

    /**
     * 查询未收到支付回调通知的订单
     *
     * @return 订单ID列表
     */
    List<String> queryNoPayNotifyOrder();

    /**
     * 查询待超时关闭的订单列表
     *
     * @return 订单ID列表
     * TODO 兜底方案：定时轮询关单，当前由 RocketMQ 延时消息处理，此方法预留备用
     */
    List<String> queryTimeoutCloseOrderList();

    /**
     * 关闭订单
     *
     * @param orderId 订单ID
     * @return 是否成功关闭
     * TODO 兜底方案：定时轮询关单，当前由 RocketMQ 延时消息处理，此方法预留备用
     */
    boolean changeOrderClose(String orderId);

    /**
     * 处理超时关单：检查订单状态 → 乐观锁关闭订单 → 恢复商品库存
     *
     * @param orderNo 订单号
     * @return 是否成功关闭
     */
    boolean handleTimeoutCloseOrder(String orderNo);
}
