package cn.fcr.infrastructure.gateway;

import cn.fcr.domain.mall.adapter.repository.IProductRepository;
import cn.fcr.domain.mall.gateway.IStockGateway;
import cn.fcr.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 库存网关实现
 * 使用 Redisson RAtomicLong 保证库存操作的原子性
 * 
 * 【Key 规范】统一使用 mall:product:stock:{productId} 格式
 * 【幂等性】使用 mall:stock:msg:processed:{messageId} 实现消息幂等检查
 */
@Slf4j
@Component
public class StockGatewayImpl implements IStockGateway {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IProductRepository productRepository;

    /**
     * 库存 Key 前缀 - 统一规范
     */
    private static final String STOCK_KEY_PREFIX = "mall:product:stock:";

    /**
     * 幂等性 Key 前缀 - 消息处理标记
     */
    private static final String IDEMPOTENT_KEY_PREFIX = "mall:stock:msg:processed:";

    /**
     * 幂等性 Key 过期时间（秒）- 24小时
     */
    private static final long IDEMPOTENT_EXPIRE_SECONDS = 86400;

    @Override
    public long deductStock(Long productId, Integer quantity) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        RAtomicLong stockCounter = redissonClient.getAtomicLong(stockKey);

        // 先读取当前库存（乐观检查）
        long currentStock = stockCounter.get();
        log.info("【库存检查】productId={}, 当前库存={}, 购买数量={}", productId, currentStock, quantity);

        // 预检查：库存不足直接拒绝
        if (currentStock < quantity) {
            throw new AppException("STOCK_INSUFFICIENT", "商品库存不足，无法下单");
        }

        // 执行原子递减操作
        long remainingStock = stockCounter.addAndGet(-quantity);

        // 二次检查：处理竞态条件（多个线程同时通过预检查的情况）
        if (remainingStock < 0) {
            // 库存不足，恢复库存并抛出异常
            stockCounter.addAndGet(quantity);
            throw new AppException("STOCK_INSUFFICIENT", "商品库存不足，无法下单");
        }

        log.info("【库存扣减成功】productId={}, 扣减后库存={}", productId, remainingStock);
        return remainingStock;
    }

    @Override
    public long restoreStock(Long productId, Integer quantity) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        RAtomicLong stockCounter = redissonClient.getAtomicLong(stockKey);

        // 执行原子递增操作
        long newStock = stockCounter.addAndGet(quantity);
        log.info("【库存恢复成功】productId={}, 恢复数量={}, 新库存={}", productId, quantity, newStock);
        return newStock;
    }

    @Override
    public long getStock(Long productId) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        RAtomicLong stockCounter = redissonClient.getAtomicLong(stockKey);
        return stockCounter.get();
    }

    @Override
    public long syncStockFromDB(Long productId) {
        // 从数据库查询库存
        Integer stockFromDB = productRepository.queryStockByProductId(productId);
        if (stockFromDB == null) {
            log.warn("【库存同步】productId={}, 商品不存在或已下架，跳过同步", productId);
            return 0;
        }

        // 设置到 Redis
        String stockKey = STOCK_KEY_PREFIX + productId;
        RAtomicLong stockCounter = redissonClient.getAtomicLong(stockKey);
        stockCounter.set(stockFromDB);

        log.info("【库存同步成功】productId={}, DB库存={}, Redis库存已同步", productId, stockFromDB);
        return stockFromDB;
    }

    @Override
    public int batchSyncStock(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            log.warn("【批量库存同步】商品ID列表为空，跳过同步");
            return 0;
        }

        int successCount = 0;
        for (Long productId : productIds) {
            try {
                long syncedStock = syncStockFromDB(productId);
                if (syncedStock >= 0) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("【批量库存同步异常】productId={}, 同步失败", productId, e);
                // 单个失败不影响整体，继续处理其他商品
            }
        }

        log.info("【批量库存同步完成】总数={}, 成功={}, 失败={}", 
                productIds.size(), successCount, productIds.size() - successCount);
        return successCount;
    }

    @Override
    public long setStock(Long productId, Integer stock) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        RAtomicLong stockCounter = redissonClient.getAtomicLong(stockKey);
        stockCounter.set(stock);

        log.info("【库存设置成功】productId={}, 新库存={}", productId, stock);
        return stock;
    }

    @Override
    public boolean checkMessageIdempotent(String messageId) {
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + messageId;
        RBucket<String> bucket = redissonClient.getBucket(idempotentKey);

        // 使用 trySet 实现 SETNX（仅在不存在时设置）
        boolean isFirstProcess = bucket.trySet("1", IDEMPOTENT_EXPIRE_SECONDS, TimeUnit.SECONDS);

        if (isFirstProcess) {
            log.info("【幂等性检查】messageId={}, 首次处理，继续执行业务逻辑", messageId);
            return true;
        } else {
            log.info("【幂等性检查】messageId={}, 已处理过，跳过执行", messageId);
            return false;
        }
    }

    @Override
    public boolean syncDBStockDeduct(Long productId, Integer quantity) {
        try {
            // 使用乐观锁扣减：只有库存充足时才扣减
            int affectedRows = productRepository.decreaseStock(productId, quantity);
            
            if (affectedRows > 0) {
                log.info("【DB库存扣减成功】productId={}, quantity={}", productId, quantity);
                return true;
            } else {
                // affectedRows = 0 表示库存不足或商品不存在
                Integer currentStock = productRepository.queryStockByProductId(productId);
                log.warn("【DB库存扣减失败】productId={}, quantity={}, 当前库存={}或商品不存在", 
                        productId, quantity, currentStock);
                return false;
            }
        } catch (Exception e) {
            log.error("【DB库存扣减异常】productId={}, quantity={}", productId, quantity, e);
            return false;
        }
    }

    @Override
    public boolean syncDBStockRestore(Long productId, Integer quantity) {
        try {
            int affectedRows = productRepository.increaseStock(productId, quantity);
            
            if (affectedRows > 0) {
                log.info("【DB库存恢复成功】productId={}, quantity={}", productId, quantity);
                return true;
            } else {
                log.warn("【DB库存恢复失败】productId={}, quantity={}, 商品可能不存在", 
                        productId, quantity);
                return false;
            }
        } catch (Exception e) {
            log.error("【DB库存恢复异常】productId={}, quantity={}", productId, quantity, e);
            return false;
        }
    }
}