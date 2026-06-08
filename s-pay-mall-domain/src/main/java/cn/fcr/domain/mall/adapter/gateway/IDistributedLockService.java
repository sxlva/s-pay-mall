package cn.fcr.domain.mall.adapter.gateway;

public interface IDistributedLockService {
    <T> T executeWithLock(Long productId, LockCallback<T> callback);

    @FunctionalInterface
    interface LockCallback<T> {
        T execute();
    }
}
