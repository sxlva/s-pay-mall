package cn.fcr.domain.mall.gateway;

import java.util.List;

/**
 * 库存网关接口
 * 提供库存预扣减和恢复的原子操作能力
 * 
 * 【DDD 原则】Domain 层定义接口，Infrastructure 层实现具体技术细节
 */
public interface IStockGateway {

    /**
     * 预扣减库存
     * 使用原子操作保证库存扣减的正确性
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return 扣减后剩余库存
     * @throws cn.fcr.types.exception.AppException 如果库存不足
     */
    long deductStock(Long productId, Integer quantity);

    /**
     * 恢复库存
     * 用于订单取消或超时关闭时恢复库存
     *
     * @param productId 商品ID
     * @param quantity  恢复数量
     * @return 恢复后库存
     */
    long restoreStock(Long productId, Integer quantity);

    /**
     * 获取当前库存
     *
     * @param productId 商品ID
     * @return 当前库存数量
     */
    long getStock(Long productId);

    /**
     * 同步单个商品库存到 Redis（从数据库读取）
     * 用于预热或缓存失效后的回填
     *
     * @param productId 商品ID
     * @return 同步后的库存值
     */
    long syncStockFromDB(Long productId);

    /**
     * 批量同步商品库存到 Redis（从数据库读取）
     * 用于应用启动时的全量预热
     *
     * @param productIds 商品ID列表
     * @return 同步成功的商品数量
     */
    int batchSyncStock(List<Long> productIds);

    /**
     * 直接设置商品库存到 Redis
     * 用于 MQ 消息同步更新
     *
     * @param productId 商品ID
     * @param stock     库存值
     * @return 设置后的库存值
     */
    long setStock(Long productId, Integer stock);

    /**
     * 检查消息是否已处理（幂等性检查）
     * 使用 Redis SETNX 实现
     *
     * @param messageId 消息唯一ID
     * @return true=首次处理，false=已处理过
     */
    boolean checkMessageIdempotent(String messageId);

    /**
     * 同步扣减 MySQL 数据库库存
     * 支付成功后调用，确保 Redis 预扣结果持久化到 MySQL
     * 使用乐观锁保证幂等性：只有库存充足时才扣减
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return true=扣减成功，false=库存不足或已扣减过
     */
    boolean syncDBStockDeduct(Long productId, Integer quantity);

    /**
     * 同步恢复 MySQL 数据库库存
     * 订单超时关闭或取消时调用，恢复已预扣的库存
     * 使用乐观锁保证幂等性
     *
     * @param productId 商品ID
     * @param quantity  恢复数量
     * @return true=恢复成功，false=商品不存在
     */
    boolean syncDBStockRestore(Long productId, Integer quantity);
}