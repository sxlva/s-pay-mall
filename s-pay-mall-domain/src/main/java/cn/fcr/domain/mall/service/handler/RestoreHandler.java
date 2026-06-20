package cn.fcr.domain.mall.service.handler;

import cn.fcr.domain.mall.gateway.IIdempotentGateway;
import cn.fcr.domain.mall.gateway.IStockGateway;
import cn.fcr.domain.mall.model.dto.StockChangeMsgDTO;
import cn.fcr.domain.mall.service.StockChangeHandler;

import java.util.logging.Logger;

/**
 * 订单取消恢复库存策略处理器
 * 处理订单取消或超时关闭时恢复 Redis 库存的业务逻辑
 *
 * 【职责】订单取消时，将之前预扣的库存释放回 Redis
 * 【触发场景】用户取消订单、订单超时未支付、支付失败等场景
 * 【幂等性】基于 businessType + businessNo（orderId）实现幂等检查
 *         幂等 Key 格式：stock:event:restore:{orderId}
 *
 * 【执行语义】
 * - tryAcquire=true: 获取锁成功，执行恢复逻辑，成功后不删除 Key（24小时自动过期）
 * - tryAcquire=false: 锁已被占用，跳过执行，返回当前库存
 * - 异常处理: 恢复失败时调用 release() 删除 Key，允许重试消费
 */
public class RestoreHandler implements StockChangeHandler {

    private final Logger logger = Logger.getLogger(RestoreHandler.class.getName());

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

        logger.info("【恢复库存策略】开始处理，productId=" + productId + ", messageId=" + messageId + ", businessNo=" + businessNo + ", quantity=" + quantity);

        // 1. 幂等性检查 - 使用业务类型 + 业务单号
        boolean acquired = idempotentGateway.tryAcquire(IIdempotentGateway.BUSINESS_TYPE_RESTORE, businessNo);

        if (!acquired) {
            // 分支 B: 锁已被占用，跳过执行，返回当前库存
            long currentStock = stockGateway.getStock(productId);
            logger.info("【恢复库存策略】幂等性检查跳过，消息已处理或正在处理。productId=" + productId + ", businessNo=" + businessNo + ", currentStock=" + currentStock);
            return currentStock;
        }

        try {
            // 分支 A: 获取锁成功，执行恢复逻辑
            logger.info("【恢复库存策略】获取幂等锁成功，开始执行恢复逻辑。productId=" + productId + ", businessNo=" + businessNo + ", quantity=" + quantity);

            // 执行 Redis 库存恢复（原子操作）
            long newStock = stockGateway.restoreStock(productId, quantity);

            logger.info("【恢复库存策略】处理成功，productId=" + productId + ", businessNo=" + businessNo + ", newStock=" + newStock);

            // 成功后不删除幂等 Key，利用 24 小时自动过期
            return newStock;

        } catch (Exception e) {
            // 异常处理：释放幂等锁，允许后续重试消费
            logger.severe("【恢复库存策略】处理异常，释放幂等锁。productId=" + productId + ", businessNo=" + businessNo + ", error=" + e.getMessage());

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
