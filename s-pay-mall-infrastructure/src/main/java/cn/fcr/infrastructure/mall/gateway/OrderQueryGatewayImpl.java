package cn.fcr.infrastructure.mall.gateway;

import cn.fcr.domain.mall.gateway.IOrderQueryGateway;
import cn.fcr.domain.mall.model.valobj.OrderSummaryVO;
import cn.fcr.domain.order.adapter.repository.IOrderRepository;
import cn.fcr.domain.shared.model.vo.PayStatus;
import cn.fcr.infrastructure.dao.order.IOrderDao;
import cn.fcr.infrastructure.dao.order.po.PayOrder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 订单查询网关基础设施实现
 * <p>
 * 【跨域桥接】位于 infrastructure/shared 层，桥接 mall 域与 order 域。
 * 实现 mall 域定义的 IOrderQueryGateway 接口，委托 order 域的 IOrderRepository 完成数据查询。
 */
@Component
public class OrderQueryGatewayImpl implements IOrderQueryGateway {

    private final IOrderRepository orderRepository;
    private final IOrderDao orderDao;

    public OrderQueryGatewayImpl(IOrderRepository orderRepository, IOrderDao orderDao) {
        this.orderRepository = orderRepository;
        this.orderDao = orderDao;
    }

    @Override
    public long countOrdersByUserId(Long userId) {
        return orderRepository.countByUserId(userId);
    }

    @Override
    public OrderSummaryVO findPayOrderByOrderNo(String orderNo) {
        PayOrder payOrder = orderDao.queryByOrderNo(orderNo);
        if (payOrder == null) {
            return null;
        }
        return toOrderSummary(payOrder);
    }

    private OrderSummaryVO toOrderSummary(PayOrder payOrder) {
        java.util.Date payTime = payOrder.getPayTime();
        LocalDateTime localPayTime = payTime != null
                ? new Timestamp(payTime.getTime()).toLocalDateTime()
                : null;

        return OrderSummaryVO.builder()
                .orderNo(payOrder.getOrderId())
                .totalAmount(payOrder.getTotalAmount())
                .payUrl(payOrder.getPayUrl())
                .status(PayStatus.fromCode(payOrder.getStatus()))
                .payTime(localPayTime)
                .build();
    }
}
