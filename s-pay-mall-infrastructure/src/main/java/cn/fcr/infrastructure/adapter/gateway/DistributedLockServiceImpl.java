package cn.fcr.infrastructure.adapter.gateway;

import cn.fcr.domain.mall.adapter.gateway.IDistributedLockService;
import cn.fcr.infrastructure.config.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class DistributedLockServiceImpl implements IDistributedLockService {

    @Resource
    private RedisDistributedLock redisDistributedLock;

    @Override
    public <T> T executeWithLock(Long productId, LockCallback<T> callback) {
        String requestId = redisDistributedLock.tryLock(productId);
        if (requestId == null) {
            throw new RuntimeException("系统繁忙，请稍后再试");
        }

        try {
            return callback.execute();
        } finally {
            redisDistributedLock.unlock(productId, requestId);
        }
    }
}
