package cn.fcr.domain.order.adapter.port;

import cn.fcr.domain.order.model.entity.PayOrderEntity;

public interface IPaymentPort {

    String generatePayUrl(PayOrderEntity payOrderEntity);

    void updatePayOrderInfo(PayOrderEntity payOrderEntity);
}