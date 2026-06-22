package cn.fcr.infrastructure.mall.gateway;

import cn.fcr.domain.mall.gateway.IIdempotentGateway;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性检查网关实现（Redisson SETNX）
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class IdempotentGatewayImpl implements IIdempotentGateway {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 幂等性 Key 前缀
     */
    private static final String IDEMPOTENT_KEY_PREFIX = "stock:event:";

    /**
     * 幂等性 Key 过期时间（小时）- 24小时
     */
    private static final long IDEMPOTENT_EXPIRE_HOURS = 24;

    /**
     * 幂等锁的值（表示正在处理中）
     */
    private static final String PROCESSING_VALUE = "PROCESSING";

    @Override
    public boolean tryAcquire(String businessType, String businessNo) {
        String idempotentKey = buildKey(businessType, businessNo);
        RBucket<String> bucket = redissonClient.getBucket(idempotentKey);

        // 使用 trySet 实现 SETNX（仅在不存在时设置）
        // 设置值为 "PROCESSING"，过期时间为 24 小时
        boolean acquired = bucket.trySet(PROCESSING_VALUE, IDEMPOTENT_EXPIRE_HOURS, TimeUnit.HOURS);

        if (acquired) {
            log.info("【幂等性检查】获取锁成功，businessType={}, businessNo={}, key={}",
                    businessType, businessNo, idempotentKey);
        } else {
            log.info("【幂等性检查】获取锁失败（正在处理或已处理），businessType={}, businessNo={}, key={}",
                    businessType, businessNo, idempotentKey);
        }

        return acquired;
    }

    @Override
    public boolean release(String businessType, String businessNo) {
        String idempotentKey = buildKey(businessType, businessNo);

        try {
            boolean deleted = redissonClient.getBucket(idempotentKey).delete();

            if (deleted) {
                log.info("【幂等性释放】释放锁成功，businessType={}, businessNo={}, key={}",
                        businessType, businessNo, idempotentKey);
            } else {
                log.warn("【幂等性释放】释放锁失败（Key 不存在），businessType={}, businessNo={}, key={}",
                        businessType, businessNo, idempotentKey);
            }

            return deleted;
        } catch (Exception e) {
            // 幂等锁释放失败不应影响主业务流程
            // 记录日志即可，Key 会在 24 小时后自动过期
            log.error("【幂等性释放】释放锁异常，businessType={}, businessNo={}, key={}, error={}",
                    businessType, businessNo, idempotentKey, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String buildKey(String businessType, String businessNo) {
        return IDEMPOTENT_KEY_PREFIX + businessType + ":" + businessNo;
    }
}
