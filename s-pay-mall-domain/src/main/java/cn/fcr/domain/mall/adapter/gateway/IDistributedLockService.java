package cn.fcr.domain.mall.adapter.gateway;

/**
 * 分布式锁服务接口，提供以商品ID为粒度的分布式锁执行能力。
 *
 * @author 傅崇睿
 */
public interface IDistributedLockService {
    /**
     * 在分布式锁保护下执行回调
     *
     * @param productId 商品ID（锁的粒度）
     * @param callback  回调逻辑
     * @param <T>       返回值类型
     * @return 回调返回值
     */
    <T> T executeWithLock(Long productId, LockCallback<T> callback);

    /**
     * 分布式锁回调接口
     */
    @FunctionalInterface
    interface LockCallback<T> {
        T execute();
    }
}
