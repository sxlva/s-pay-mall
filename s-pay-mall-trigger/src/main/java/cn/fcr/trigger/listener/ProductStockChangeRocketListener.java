package cn.fcr.trigger.listener;

import cn.fcr.domain.mall.model.dto.StockChangeMessageDTO;
import cn.fcr.domain.mall.model.dto.StockChangeMsg;
import cn.fcr.domain.mall.service.StockChangeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 库存变更消息消费者
 * 监听 product-stock-change-topic，通过策略模式路由到对应的处理器
 *
 * 【策略模式】
 * - 通过 List<StockChangeHandler> 自动匹配对应的变更类型
 * - 每个 Handler 只处理一种变更类型（单一职责）
 * - 新增变更类型只需添加新的 Handler，无需修改此类
 *
 * 【消息来源】
 * - 后台管理员修改库存（ADMIN_UPDATE）-> AdminUpdateHandler
 * - 支付成功扣减库存（PAY_DEDUCT）-> DeductHandler
 * - 订单取消恢复库存（ORDER_RESTORE）-> RestoreHandler
 *
 * 【日志规范】所有日志必须包含 productId 和 messageId，便于生产环境排查
 *
 * 【消费语义】
 * - 正常处理：返回 void（自动确认消费成功）
 * - 处理异常：抛出 RuntimeException（触发 MQ 重试）
 * - 无匹配 Handler：记录 Warn 日志并丢弃消息（配置问题，重试无效）
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "product-stock-change-topic",
        consumerGroup = "s-pay-mall-stock-change-consumer"
)
public class ProductStockChangeRocketListener implements RocketMQListener<StockChangeMessageDTO> {

    @Resource
    private List<StockChangeHandler> stockChangeHandlers;

    @Override
    public void onMessage(StockChangeMessageDTO message) {
        Long productId = message.getProductId();
        String messageId = message.getMessageId();
        String changeType = message.getChangeType();

        log.info("【库存变更消息】收到消息，productId={}, messageId={}, changeType={}",
                productId, messageId, changeType);

        try {
            // 1. 转换为内部消息格式
            StockChangeMsg stockChangeMsg = convertToStockChangeMsg(message);

            // 2. 查找匹配的策略处理器
            StockChangeHandler handler = findHandler(changeType);

            if (handler == null) {
                // 无匹配 Handler，记录 Warn 日志并丢弃消息
                // 【设计决策】无匹配 Handler 属于配置问题，重试无法解决，直接记录并丢弃
                log.warn("【库存变更消息】未找到对应的处理器，已丢弃消息。productId={}, messageId={}, changeType={}",
                        productId, messageId, changeType);
                // 不抛出异常，消息将被确认消费成功（丢弃）
                // 如果需要放入死信队列，需要手动发送到死信 Topic
                return;
            }

            log.info("【库存变更消息】匹配到处理器，handler={}, productId={}, messageId={}",
                    handler.getClass().getSimpleName(), productId, messageId);

            // 3. 执行处理
            long resultStock = handler.handle(productId, stockChangeMsg);

            log.info("【库存变更消息】处理成功，productId={}, messageId={}, changeType={}, resultStock={}",
                    productId, messageId, changeType, resultStock);

            // RocketMQListener<T> 接口：正常返回 void 即表示 CONSUME_SUCCESS
            // 无需显式返回 ConsumeConcurrentlyStatus.CONSUME_SUCCESS

        } catch (Exception e) {
            log.error("【库存变更消息】处理失败，productId={}, messageId={}, changeType={}, error={}",
                    productId, messageId, changeType, e.getMessage(), e);

            // 抛出异常触发 MQ 重试机制
            // 异常会导致消息重新投递到队列，进行重试
            throw new RuntimeException("库存变更消息处理失败，productId=" + productId + ", messageId=" + messageId, e);
        }
    }

    /**
     * 转换消息格式
     * 【消息格式要求】
     * - ADMIN_UPDATE: 只需 newStock（全量库存值），changeQuantity 可为 0 或 null
     * - PAY_DEDUCT: 只需 changeQuantity（扣减数量，负数），newStock 可为 null
     * - ORDER_RESTORE: 只需 changeQuantity（恢复数量，正数），newStock 可为 null
     * 【幂等性】businessNo 为业务单号（orderId 或 updateRecordId），用于幂等性检查
     */
    private StockChangeMsg convertToStockChangeMsg(StockChangeMessageDTO message) {
        return StockChangeMsg.builder()
                .productId(message.getProductId())
                .changeQuantity(message.getChangeQuantity())
                .newStock(message.getNewStock())
                .changeType(message.getChangeType())
                .messageId(message.getMessageId())
                .businessNo(message.getBusinessNo())
                .build();
    }

    /**
     * 根据变更类型查找对应的策略处理器
     * 通过 supports() 方法进行匹配
     *
     * @return 匹配的 Handler，如果没有匹配返回 null
     */
    private StockChangeHandler findHandler(String changeType) {
        for (StockChangeHandler handler : stockChangeHandlers) {
            if (handler.supports(changeType)) {
                return handler;
            }
        }
        log.debug("【策略匹配】未找到匹配的 Handler，changeType={}", changeType);
        return null;
    }
}
