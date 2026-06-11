package cn.fcr.domain.order.service;

import cn.fcr.domain.order.gateway.IPaymentGateway;
import cn.fcr.domain.order.gateway.IProductGateway;
import cn.fcr.domain.order.adapter.repository.IOrderRepository;
import cn.fcr.domain.order.model.aggregate.CreateOrderAggregate;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.valobj.PayStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class OrderService extends AbstractOrderService {

    public OrderService(IOrderRepository repository, IProductGateway productGateway, IPaymentGateway paymentGateway) {
        super(repository, productGateway, paymentGateway);
    }

    @Override
    protected void doSaveOrder(CreateOrderAggregate orderAggregate) {
        repository.doSaveOrder(orderAggregate);
    }

    @Override
    protected PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount) throws Exception {
        PayOrderEntity payOrderEntity = PayOrderEntity.builder()
                .userId(userId)
                .orderNo(orderId)
                .productId(productId)
                .productName(productName)
                .totalAmount(totalAmount)
                .status(PayStatus.WAIT_PAY)
                .build();

        String payUrl = paymentGateway.generatePayUrl(payOrderEntity);
        payOrderEntity.initPayUrl(payUrl);

        repository.updateOrderPayInfo(payOrderEntity);

        return payOrderEntity;
    }

    @Override
    public void changeOrderPaySuccess(String orderId) {
        repository.changeOrderPaySuccess(orderId);
    }

    @Override
    public List<String> queryNoPayNotifyOrder() {
        return repository.queryNoPayNotifyOrder();
    }

    @Override
    public List<String> queryTimeoutCloseOrderList() {
        return repository.queryTimeoutCloseOrderList();
    }

    @Override
    public boolean changeOrderClose(String orderId) {
        return repository.changeOrderClose(orderId);
    }

    // 【DDD 重构】handleTimeoutCloseOrder 已迁移至 AbstractOrderService
    // 父类方法已使用 order.closeByTimeout() 进行状态变更，无需重复实现
}