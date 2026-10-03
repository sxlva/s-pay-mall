package cn.fcr.infrastructure.order.gateway;

import cn.fcr.domain.order.gateway.IOrderQueryGateway;
import cn.fcr.domain.order.model.valobj.OrderSummaryVO;
import cn.fcr.domain.order.model.vo.PayStatus;
import cn.fcr.infrastructure.dao.order.IOrderDao;
import cn.fcr.infrastructure.dao.order.IOrderMainDao;
import cn.fcr.infrastructure.dao.order.po.OrderMain;
import cn.fcr.infrastructure.dao.order.po.PayOrder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 订单查询网关基础设施实现（跨域桥接）
 *
 * @author 傅崇睿
 */
@Component
public class OrderQueryGatewayImpl implements IOrderQueryGateway {

    private final IOrderMainDao orderMainDao;
    private final IOrderDao orderDao;

    public OrderQueryGatewayImpl(IOrderMainDao orderMainDao, IOrderDao orderDao) {
        this.orderMainDao = orderMainDao;
        this.orderDao = orderDao;
    }

    @Override
    public long countOrdersByUserId(Long userId) {
        // 口径：order_main 新链订单数（旧链下线前该实现原样位于 legacy 仓储，语义不变）
        LambdaQueryWrapper<OrderMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderMain::getUserId, userId);
        return orderMainDao.selectCount(wrapper);
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
