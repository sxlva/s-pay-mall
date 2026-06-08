package cn.fcr.domain.mall.gateway;

import cn.fcr.domain.mall.model.entity.OrderEntity;
import cn.fcr.domain.mall.model.valobj.OrderVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商城订单查询网关
 * 提供订单查询、统计等只读操作
 */
public interface IMallOrderQueryGateway {

    void saveOrder(OrderEntity orderEntity);

    List<OrderVO> findOrders(Long userId, String status, String start, String end);

    OrderEntity findById(Long id);

    OrderEntity findByOrderNo(String orderNo);

    int deleteById(Long id);

    int updateOrderStatus(Long orderId, String status);

    int updateOrderStatusByOrderNo(String orderNo, String status);

    BigDecimal sumDailySales(String date);

    Integer countDailyOrders(String date);
}