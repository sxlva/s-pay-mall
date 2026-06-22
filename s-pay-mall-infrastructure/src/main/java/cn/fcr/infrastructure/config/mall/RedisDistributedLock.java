package cn.fcr.infrastructure.config.mall;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis SETNX的轻量级分布式锁
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class RedisDistributedLock {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String LOCK_PREFIX = "lock:mall:cart:update:";
    private static final long LOCK_EXPIRE_TIME = 10;

    /**
     * 尝试获取分布式锁
     *
     * @param productId 商品ID，作为锁的维度
     * @return 锁的requestId（获取成功）或null（获取失败）
     */
    public String tryLock(Long productId) {
        String lockKey = LOCK_PREFIX + productId;
        String requestId = UUID.randomUUID().toString();

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, requestId, LOCK_EXPIRE_TIME, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(success)) {
            log.info("获取锁成功: lockKey={}, requestId={}", lockKey, requestId);
            return requestId;
        }
        log.warn("获取锁失败: lockKey={}", lockKey);
        return null;
    }

    /**
     * 释放分布式锁
     *
     * @param productId 商品ID
     * @param requestId 锁的requestId，用于校验锁的归属
     */
    public void unlock(Long productId, String requestId) {
        String lockKey = LOCK_PREFIX + productId;
        try {
            String currentRequestId = stringRedisTemplate.opsForValue().get(lockKey);
            if (requestId.equals(currentRequestId)) {
                stringRedisTemplate.delete(lockKey);
                log.info("释放锁成功: lockKey={}", lockKey);
            } else {
                log.warn("锁已被其他线程持有或已过期: lockKey={}", lockKey);
            }
        } catch (Exception e) {
            log.error("释放锁异常: lockKey={}", lockKey, e);
        }
    }
}
