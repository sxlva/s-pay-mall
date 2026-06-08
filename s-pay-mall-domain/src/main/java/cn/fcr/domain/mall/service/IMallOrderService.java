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
}