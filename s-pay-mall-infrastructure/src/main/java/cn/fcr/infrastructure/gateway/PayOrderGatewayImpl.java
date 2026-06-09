package cn.fcr.infrastructure.gateway;

import cn.fcr.domain.mall.gateway.IPayOrderGateway;
import cn.fcr.domain.order.model.valobj.PayStatus;
import cn.fcr.infrastructure.dao.IOrderDao;
import cn.fcr.infrastructure.dao.po.PayOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 支付订单网关实现
 */
@Slf4j
@Component
public class PayOrderGatewayImpl implements IPayOrderGateway {

    @Resource
    private IOrderDao orderDao;

    @Override
    public boolean updatePayStatusToPaid(String orderNo) {
        PayOrder payOrder = orderDao.queryByOrderNo(orderNo);
        if (payOrder == null) {
            log.warn("支付订单不存在，orderNo={}", orderNo);
            return false;
        }
        
        payOrder.setStatus(PayStatus.PAID.getCode());
        payOrder.setPayTime(new Date());
        payOrder.setUpdateTime(new Date());
        orderDao.changeOrderPaySuccess(payOrder);
        
        log.info("支付订单状态更新为已支付，orderNo={}", orderNo);
        return true;
    }

    @Override
    public boolean updatePayStatusToTradeDone(String orderNo) {
        PayOrder payOrder = orderDao.queryByOrderNo(orderNo);
        if (payOrder == null) {
            log.warn("支付订单不存在，orderNo={}", orderNo);
            return false;
        }
        
        payOrder.setStatus(PayStatus.TRADE_DONE.getCode());
        payOrder.setUpdateTime(new Date());
        orderDao.updateOrderPayInfo(payOrder);
        
        log.info("支付订单状态更新为交易完成，orderNo={}", orderNo);
        return true;
    }

    @Override
    public boolean closePayOrder(String orderNo) {
        orderDao.changeOrderClose(orderNo);
        log.info("支付订单已关闭，orderNo={}", orderNo);
        return true;
    }

    @Override
    public PayStatus getPayStatus(String orderNo) {
        String status = orderDao.queryOrderStatus(orderNo);
        return PayStatus.fromCode(status);
    }
}