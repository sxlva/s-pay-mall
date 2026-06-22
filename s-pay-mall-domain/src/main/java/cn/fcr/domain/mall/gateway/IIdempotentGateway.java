package cn.fcr.domain.mall.gateway;

/**
 * 幂等性检查网关接口，提供消息幂等性检查能力，防止重复处理。
 * 幂等 Key 格式统一为 stock:event:{业务类型}:{业务单号}，过期时间 24 小时。
 *
 * @author 傅崇睿
 */
public interface IIdempotentGateway {

    /**
     * 业务类型常量
     */
    String BUSINESS_TYPE_DEDUCT = "deduct";
    String BUSINESS_TYPE_RESTORE = "restore";
    String BUSINESS_TYPE_ADMIN_UPDATE = "admin_update";

    /**
     * 尝试获取幂等锁
     * 使用 Redis SETNX 实现，仅在 key 不存在时设置成功
     *
     * @param businessType 业务类型（如 deduct、restore、admin_update）
     * @param businessNo   业务单号（如 orderId、updateRecordId）
     * @return true=获取锁成功（可以继续执行），false=锁已被占用（跳过执行）
     */
    boolean tryAcquire(String businessType, String businessNo);

    /**
     * 释放幂等锁
     * 在业务执行异常时调用，删除幂等 Key，允许后续重试消费
     *
     * @param businessType 业务类型
     * @param businessNo   业务单号
     * @return true=删除成功，false=Key 不存在
     */
    boolean release(String businessType, String businessNo);

    /**
     * 构建幂等 Key
     *
     * @param businessType 业务类型
     * @param businessNo   业务单号
     * @return 幂等 Key（格式：stock:event:{businessType}:{businessNo}）
     */
    String buildKey(String businessType, String businessNo);
}
