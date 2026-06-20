package cn.fcr.trigger.application;

import cn.fcr.domain.mall.service.IMallOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单应用层服务
 * <p>
 * 【DDD 应用层】负责事务编排，协调领域对象完成业务操作。
 * 事务边界统一在此层控制，领域层不感知事务。
 */
@Slf4j
@Service
public class OrderApplicationService {

    private final IMallOrderService mallOrderService;

    public OrderApplicationService(IMallOrderService mallOrderService) {
        this.mallOrderService = mallOrderService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo) {
        mallOrderService.paySuccess(orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deliverOrder(Long orderId) {
        return mallOrderService.deliverOrder(orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int cancelOrder(Long orderId) {
        return mallOrderService.cancelOrder(orderId);
    }
}
