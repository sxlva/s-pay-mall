package cn.fcr.infrastructure.gateway;

import cn.fcr.domain.order.gateway.IPaymentGateway;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.service.PayOrderService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author 傅崇睿
 * @description 支付网关实现
 */
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
    }
}