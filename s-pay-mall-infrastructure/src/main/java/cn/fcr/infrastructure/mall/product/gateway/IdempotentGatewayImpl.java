package cn.fcr.infrastructure.mall.product.gateway;

import cn.fcr.domain.mall.product.gateway.IIdempotentGateway;
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
        return tryAcquire(businessType, businessNo, IDEMPOTENT_EXPIRE_HOURS * 3600);
    }

    @Override
    public boolean tryAcquire(String businessType, String businessNo, long ttlSeconds) {
        String idempotentKey = buildKey(businessType, businessNo);
        RBucket<String> bucket = redissonClient.getBucket(idempotentKey);

        // 使用 trySet 实现 SETNX（仅在不存在时设置）
        boolean acquired = bucket.trySet(PROCESSING_VALUE, ttlSeconds, TimeUnit.SECONDS);

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
    public void markDone(String businessType, String businessNo, String resultValue) {
        String idempotentKey = buildKey(businessType, businessNo);
        try {
            // 直接覆盖值并续期 24h；此时持有者为本线程，无并发写冲突
            redissonClient.getBucket(idempotentKey).set(resultValue, IDEMPOTENT_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("【幂等性完成】业务执行完成，结果已记录，businessType={}, businessNo={}, key={}, result={}",
                    businessType, businessNo, idempotentKey, resultValue);
        } catch (Exception e) {
            // 标记失败不影响主流程（重复请求会走"处理中"分支）
            log.error("【幂等性完成】标记完成异常，businessType={}, businessNo={}, key={}, error={}",
                    businessType, businessNo, idempotentKey, e.getMessage(), e);
        }
    }

    @Override
    public String getValue(String businessType, String businessNo) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(buildKey(businessType, businessNo));
            return bucket.get();
        } catch (Exception e) {
            log.error("【幂等性查询】读取幂等值异常，businessType={}, businessNo={}, error={}",
                    businessType, businessNo, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String buildKey(String businessType, String businessNo) {
        return IDEMPOTENT_KEY_PREFIX + businessType + ":" + businessNo;
    }
}
