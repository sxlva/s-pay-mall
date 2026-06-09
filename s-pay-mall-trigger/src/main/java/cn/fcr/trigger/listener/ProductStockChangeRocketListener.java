package cn.fcr.trigger.listener;

import cn.fcr.domain.mall.gateway.IStockGateway;
import cn.fcr.domain.mall.model.dto.StockChangeMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 库存变更消息消费者
 * 监听 product-stock-change-topic，同步更新 Redis 库存
 * 
 * 【消息来源】
 * - 后台管理员修改库存（ADMIN_UPDATE）
 * - 支付成功扣减库存（PAY_DEDUCT）
 * - 订单取消恢复库存（ORDER_RESTORE）
 * 
 * 【幂等性】基于 Redis SETNX 实现消息幂等检查，防止重复消费
 * 【日志规范】所有日志必须包含 productId 和 messageId，便于生产环境排查
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "product-stock-change-topic",
        consumerGroup = "s-pay-mall-stock-change-consumer"
)
public class ProductStockChangeRocketListener implements RocketMQListener<StockChangeMessageDTO> {

    @Resource
    private IStockGateway stockGateway;

    @Override
    public void onMessage(StockChangeMessageDTO message) {
        Long productId = message.getProductId();
        String messageId = message.getMessageId();
        Integer newStock = message.getNewStock();
        String changeType = message.getChangeType();

        log.info("【库存变更消息】收到消息，productId={}, messageId={}, changeType={}, newStock={}",
                productId, messageId, changeType, newStock);

        try {
            // 1. 幂等性检查
            boolean isFirstProcess = stockGateway.checkMessageIdempotent(messageId);
            
            if (!isFirstProcess) {
                log.info("【库存变更消息】幂等性检查通过，消息已处理过，跳过执行。productId={}, messageId={}",
                        productId, messageId);
                return;
            }

            // 2. 同步更新 Redis 库存
            long syncedStock = stockGateway.setStock(productId, newStock);
            
            log.info("【库存变更消息】Redis库存同步成功。productId={}, messageId={}, changeType={}, syncedStock={}",
                    productId, messageId, changeType, syncedStock);

        } catch (Exception e) {
            log.error("【库存变更消息】处理失败，productId={}, messageId={}, changeType={}, newStock={}",
                    productId, messageId, changeType, newStock, e);
            
            // 抛出异常触发 MQ 重试机制
            throw new RuntimeException("库存变更消息处理失败，productId=" + productId + ", messageId=" + messageId, e);
        }
    }
}