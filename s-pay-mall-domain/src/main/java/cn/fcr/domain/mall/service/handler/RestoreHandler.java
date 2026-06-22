package cn.fcr.domain.mall.service.handler;

import cn.fcr.domain.mall.gateway.IIdempotentGateway;
import cn.fcr.domain.mall.gateway.IStockGateway;
import cn.fcr.domain.mall.model.dto.StockChangeMsgDTO;
import cn.fcr.domain.mall.service.StockChangeHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * 订单取消或超时关闭时恢复 Redis 库存的策略处理器。
 * 幂等 Key 格式：stock:event:restore:{orderId}。
 *
 * @author 傅崇睿
 */
@Slf4j
public class RestoreHandler implements StockChangeHandler {

    private final IIdempotentGateway idempotentGateway;
    private final IStockGateway stockGateway;

    public RestoreHandler(IIdempotentGateway idempotentGateway, IStockGateway stockGateway) {
        this.idempotentGateway = idempotentGateway;
        this.stockGateway = stockGateway;
    }

    @Override
    public long handle(Long productId, StockChangeMsgDTO msg) {
        String messageId = msg.getMessageId();
        String businessNo = msg.getBusinessNo();
        Integer quantity = msg.getChangeQuantity();

        log.info("【恢复库存策略】开始处理，productId=" + productId + ", messageId=" + messageId + ", businessNo=" + businessNo + ", quantity=" + quantity);

        // 1. 幂等性检查 - 使用业务类型 + 业务单号
        boolean acquired = idempotentGateway.tryAcquire(IIdempotentGateway.BUSINESS_TYPE_RESTORE, businessNo);

        if (!acquired) {
            // 分支 B: 锁已被占用，跳过执行，返回当前库存
            long currentStock = stockGateway.getStock(productId);
            log.info("【恢复库存策略】幂等性检查跳过，消息已处理或正在处理。productId=" + productId + ", businessNo=" + businessNo + ", currentStock=" + currentStock);
            return currentStock;
        }

        try {
            // 分支 A: 获取锁成功，执行恢复逻辑
            log.info("【恢复库存策略】获取幂等锁成功，开始执行恢复逻辑。productId=" + productId + ", businessNo=" + businessNo + ", quantity=" + quantity);

            // 执行 Redis 库存恢复（原子操作）
            long newStock = stockGateway.restoreStock(productId, quantity);

            log.info("【恢复库存策略】处理成功，productId=" + productId + ", businessNo=" + businessNo + ", newStock=" + newStock);

            // 成功后不删除幂等 Key，利用 24 小时自动过期
            return newStock;

        } catch (Exception e) {
            // 异常处理：释放幂等锁，允许后续重试消费
            log.error("【恢复库存策略】处理异常，释放幂等锁。productId=" + productId + ", businessNo=" + businessNo + ", error=" + e.getMessage());

            idempotentGateway.release(IIdempotentGateway.BUSINESS_TYPE_RESTORE, businessNo);

            // 抛出异常触发 MQ 重试机制
            throw new RuntimeException("恢复库存失败，productId=" + productId + ", businessNo=" + businessNo, e);
        }
    }

    @Override
    public boolean supports(String changeType) {
        return StockChangeMsgDTO.CHANGE_TYPE_ORDER_RESTORE.equals(changeType);
    }

    @Override
    public String getSupportedChangeType() {
        return StockChangeMsgDTO.CHANGE_TYPE_ORDER_RESTORE;
    }
}
