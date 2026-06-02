package cn.fcr.domain.mall.adapter.port;

import cn.fcr.domain.mall.model.valobj.CartItemVO;
import cn.fcr.domain.mall.model.valobj.OrderCreateVO;
import cn.fcr.domain.mall.model.valobj.OrderVO;
import cn.fcr.domain.order.model.entity.PayOrderEntity;

import java.util.List;

public interface IOrderPaymentPort {

    String generatePayUrl(PayOrderEntity payOrderEntity);

    void updatePayOrderInfo(PayOrderEntity payOrderEntity);

    void sendDelayCloseMessage(String orderNo);
}