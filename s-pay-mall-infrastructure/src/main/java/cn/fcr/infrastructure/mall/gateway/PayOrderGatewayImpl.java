package cn.fcr.infrastructure.mall.gateway;

import cn.fcr.domain.mall.gateway.IPayOrderGateway;
import cn.fcr.domain.shared.model.vo.PayStatus;
import cn.fcr.infrastructure.dao.IOrderDao;
import cn.fcr.infrastructure.dao.po.PayOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @description 支付订单网关实现类
 *
 * 【职责说明】
 * - 处理支付订单状态的持久化操作
 * - 实现领域层定义的IPayOrderGateway接口
 * - 负责更新支付状态、关闭订单等操作
 *
 * 【核心功能】
 * 1. updatePayStatusToPaid(): 更新支付状态为已支付
 * 2. updatePayStatusToTradeDone(): 更新支付状态为交易完成
 * 3. closePayOrder(): 关闭支付订单
 * 4. getPayStatus(): 获取支付状态
 *
 * 【依赖说明】
 * - IOrderDao: 订单数据访问接口
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
