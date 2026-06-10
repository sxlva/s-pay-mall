package cn.fcr.infrastructure.gateway;

import cn.fcr.domain.order.gateway.IPaymentGateway;
import cn.fcr.domain.order.model.entity.PayOrderEntity;
import cn.fcr.domain.order.service.PayOrderService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @description 支付网关实现
 * 
 * 【职责说明】
 * - 处理支付相关的网关操作
 * - 实现领域层定义的IPaymentGateway接口
 * - 负责生成支付URL、更新支付订单信息等
 * 
 * 【核心功能】
 * 1. generatePayUrl(): 生成支付URL
 * 2. updatePayOrderInfo(): 更新支付订单信息
 * 
 * 【依赖说明】
 * - PayOrderService: 支付订单服务
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