package cn.fcr.domain.order.service;

import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.entity.ShopCartEntity;

import java.util.List;

/**
 * @author 傅崇睿
 * @date 2025/7/28 19:27
 * @description 订单服务接口
 */
public interface IOrderService {

    PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception;

    void changeOrderPaySuccess(String orderId);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId);

    /**
     * 处理超时关单
     * 1. 检查订单状态
     * 2. 乐观锁方式关闭订单
     * 3. 恢复商品库存
     *
     * @param orderNo 订单号
     * @return 是否成功关闭
     */
    boolean handleTimeoutCloseOrder(String orderNo);
}
