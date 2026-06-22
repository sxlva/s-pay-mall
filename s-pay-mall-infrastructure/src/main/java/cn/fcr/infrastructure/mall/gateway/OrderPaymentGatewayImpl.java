package cn.fcr.infrastructure.mall.gateway;

import cn.fcr.domain.mall.gateway.IOrderPaymentGateway;
import cn.fcr.domain.shared.model.entity.PayOrderEntity;
import cn.fcr.domain.order.service.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @description 订单支付网关实现
 *
 * 【职责说明】
 * - 处理订单支付相关的网关操作
 * - 实现领域层定义的IOrderPaymentGateway接口
 * - 负责生成支付URL、更新支付订单信息、发送延时关闭消息等
 *
 * 【核心功能】
 * 1. generatePayUrl(): 生成支付URL
 * 2. updatePayOrderInfo(): 更新支付订单信息
 * 3. sendDelayCloseMessage(): 发送订单延时关闭消息
 *
 * 【依赖说明】
 * - PayOrderService: 支付订单服务
 * - RocketMQTemplate: RocketMQ消息发送模板
 */
@Slf4j
@Component
public class OrderPaymentGatewayImpl implements IOrderPaymentGateway {

    @Resource
    private PayOrderService payOrderService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public String generatePayUrl(PayOrderEntity payOrderEntity) {
        return payOrderService.generatePayUrl(payOrderEntity);
    }

    @Override
    public void updatePayOrderInfo(PayOrderEntity payOrderEntity) {
        // TODO: 待实现 - 更新支付订单信息
        log.warn("updatePayOrderInfo 暂未实现，orderNo={}", payOrderEntity.getOrderNo());
    }

    @Override
    public void sendDelayCloseMessage(String orderNo) {
        log.info("发送订单延时关闭消息: orderNo={}", orderNo);
        try {
            rocketMQTemplate.syncSend("order-timeout-topic", orderNo, 3000);
            log.info("订单延时关闭消息发送成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("发送订单延时关闭消息失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
        }
    }
}
