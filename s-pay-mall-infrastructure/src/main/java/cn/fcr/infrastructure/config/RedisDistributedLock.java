package cn.fcr.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisDistributedLock {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String LOCK_PREFIX = "lock:mall:cart:update:";
    private static final long LOCK_EXPIRE_TIME = 10;

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
