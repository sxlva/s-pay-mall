package cn.fcr.trigger.listener;

import cn.fcr.domain.mall.model.dto.StockChangeMsgDTO;
import cn.fcr.domain.mall.service.StockChangeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 库存变更 RocketMQ 消息监听器，通过策略模式路由到对应的变更处理器。
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "product-stock-change-topic",
        consumerGroup = "s-pay-mall-stock-change-consumer"
)
public class ProductStockChangeRocketListener implements RocketMQListener<StockChangeMsgDTO> {

    /** 库存变更策略处理器列表，Spring 自动注入所有 StockChangeHandler 实现 */
    @Resource
    private List<StockChangeHandler> stockChangeHandlers;

    /**
     * 消费库存变更消息，按变更类型匹配策略处理器并执行。
     *
     * @param message 库存变更消息体
     */
    @Override
    public void onMessage(StockChangeMsgDTO message) {
        Long productId = message.getProductId();
        String messageId = message.getMessageId();
        String changeType = message.getChangeType();

        log.info("【库存变更消息】收到消息，productId={}, messageId={}, changeType={}",
                productId, messageId, changeType);

        try {
            // 1. 查找匹配的策略处理器
            StockChangeHandler handler = findHandler(changeType);

            if (handler == null) {
                log.warn("【库存变更消息】未找到对应的处理器，已丢弃消息。productId={}, messageId={}, changeType={}",
                        productId, messageId, changeType);
                return;
            }

            log.info("【库存变更消息】匹配到处理器，handler={}, productId={}, messageId={}",
                    handler.getClass().getSimpleName(), productId, messageId);

            // 2. 执行处理
            long resultStock = handler.handle(productId, message);

            log.info("【库存变更消息】处理成功，productId={}, messageId={}, changeType={}, resultStock={}",
                    productId, messageId, changeType, resultStock);

        } catch (Exception e) {
            log.error("【库存变更消息】处理失败，productId={}, messageId={}, changeType={}, error={}",
                    productId, messageId, changeType, e.getMessage(), e);

            throw new RuntimeException("库存变更消息处理失败，productId=" + productId + ", messageId=" + messageId, e);
        }
    }

    /**
     * 根据变更类型查找对应的策略处理器
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
