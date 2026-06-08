package cn.fcr.domain.order.service;

import cn.fcr.domain.order.gateway.IPaymentGateway;
import cn.fcr.domain.order.gateway.IProductGateway;
import cn.fcr.domain.order.adapter.repository.IOrderRepository;
import cn.fcr.domain.order.model.aggregate.CreateOrderAggregate;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.model.valobj.OrderStatusVO;
import cn.fcr.domain.order.model.valobj.PayStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
        payOrderEntity.setPayUrl(payUrl);

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

    @Override
    public boolean handleTimeoutCloseOrder(String orderNo) {
        log.info("处理超时关单: orderNo={}", orderNo);

        String currentStatus = repository.queryOrderStatus(orderNo);
        if (currentStatus == null) {
            log.warn("订单不存在，可能已被删除: orderNo={}", orderNo);
            return false;
        }

        if (!OrderStatusVO.CREATE.getCode().equals(currentStatus)) {
            log.info("订单状态已变更，无需关单: orderNo={}, status={}", orderNo, currentStatus);
            return false;
        }

        boolean closed = repository.closeOrderWithOptimisticLock(orderNo, OrderStatusVO.CREATE.getCode());
        if (!closed) {
            log.info("订单状态已被其他线程修改，关单失败: orderNo={}", orderNo);
            return false;
        }

        List<Map<String, Object>> orderItems = repository.queryOrderItems(orderNo);
        for (Map<String, Object> item : orderItems) {
            String productId = (String) item.get("productId");
            Integer quantity = (Integer) item.get("quantity");
            productGateway.restoreStock(productId, quantity);
            log.info("已恢复库存: productId={}, quantity={}", productId, quantity);
        }

        log.info("超时关单成功: orderNo={}", orderNo);
        return true;
    }
}