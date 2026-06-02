package cn.fcr.infrastructure.adapter.port;

import cn.fcr.domain.order.adapter.port.IPaymentPort;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.service.PayOrderService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class PaymentPortImpl implements IPaymentPort {

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