package cn.fcr.infrastructure.order.gateway;

import cn.fcr.domain.order.gateway.IPaymentGateway;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.domain.order.service.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 支付网关实现
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class PaymentGatewayImpl implements IPaymentGateway {

    @Resource
    private PayOrderService payOrderService;

    @Override
    public String generatePayUrl(PayOrderEntity payOrderEntity) {
        return payOrderService.generatePayUrl(payOrderEntity);
    }

    @Override
    public void updatePayOrderInfo(PayOrderEntity payOrderEntity) {
        // 暂未实现：更新支付订单信息（需要补充数据库持久化逻辑）
        log.warn("updatePayOrderInfo 暂未实现，orderNo={}", payOrderEntity.getOrderNo());
    }
}
