package cn.fcr.domain.mall.service;

import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;

import java.util.List;

/**
 * 订单领域服务接口
 */
public interface IMallOrderService {

    OrderCreateVO createOrder(Long userId, String address);

    List<OrderVO> listOrders(Long userId, String status, String start, String end);

    int deleteOrder(Long id);

    int deliverOrder(Long orderId);

    int cancelOrder(Long orderId);

    void paySuccess(String orderNo);

    OrderVO getOrderByNo(String orderNo);

    /**
     * 继续支付订单
     * 根据订单号重新生成支付链接，用于未支付订单的继续支付场景
     *
     * @param orderNo 订单号
     * @return 订单创建结果（包含支付链接）
     */
    OrderCreateVO continuePay(String orderNo);

    /**
     * 检查订单商品库存
     * 用于继续支付前的库存同步校验
     *
     * @param orderNo 订单号
     * @return true=库存充足，false=库存不足
     */
    boolean checkOrderStock(String orderNo);
}