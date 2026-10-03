package cn.fcr.domain.mall.product.gateway;

/**
 * 幂等性检查网关接口，提供业务幂等性检查能力，防止重复处理。
 * 基于 Redis SETNX 实现：同一业务类型 + 业务单号在过期窗口内仅允许执行一次。
 * 幂等 Key 格式统一为 stock:event:{业务类型}:{业务单号}（前缀为历史兼容保留）。
 *
 * <p>当前使用者：
 * <ul>
 *   <li>库存变更 MQ 三处理器（deduct / restore / admin_update）</li>
 *   <li>订单创建幂等（order_create，P0-4）</li>
 *   <li>支付成功通知消费幂等（order_paid_notify，P0-4）</li>
 * </ul>
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
    /** 订单创建幂等（P0-4）：业务单号为客户端 requestId，或降级为 uid:{userId}） */
    String BUSINESS_TYPE_ORDER_CREATE = "order_create";
    /** 支付成功通知消费幂等（P0-4）：业务单号为 orderNo */
    String BUSINESS_TYPE_ORDER_PAID_NOTIFY = "order_paid_notify";

    /**
     * 处理中标记值（tryAcquire 成功后写入，markDone 后改写为业务结果）
     */
    String PROCESSING_VALUE = "PROCESSING";

    /**
     * 尝试获取幂等锁（默认 24 小时过期）
     * 使用 Redis SETNX 实现，仅在 key 不存在时设置成功
     *
     * @param businessType 业务类型（如 deduct、restore、admin_update、order_create）
     * @param businessNo   业务单号（如 orderId、updateRecordId、requestId）
     * @return true=获取锁成功（可以继续执行），false=锁已被占用（跳过执行）
     */
    boolean tryAcquire(String businessType, String businessNo);

    /**
     * 尝试获取幂等锁（自定义过期时间）
     * 用于短窗口防重场景（如双击防护），过期后允许再次执行
     *
     * @param businessType 业务类型
     * @param businessNo   业务单号
     * @param ttlSeconds   过期时间（秒）
     * @return true=获取锁成功，false=锁已被占用
     */
    boolean tryAcquire(String businessType, String businessNo, long ttlSeconds);

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
     * 标记业务执行完成（P0-4）
     * 将幂等 Key 的值由 PROCESSING 改写为业务结果（如 orderNo），
     * 使重复请求可查询到首次执行的结果，实现"同键返回同单"语义
     *
     * @param businessType 业务类型
     * @param businessNo   业务单号
     * @param resultValue  业务结果值（如订单号）
     */
    void markDone(String businessType, String businessNo, String resultValue);

    /**
     * 读取幂等 Key 的当前值（P0-4）
     * 用于重复请求查询首次执行结果；PROCESSING 表示仍在处理中
     *
     * @param businessType 业务类型
     * @param businessNo   业务单号
     * @return 当前值（PROCESSING / 业务结果值），Key 不存在返回 null
     */
    String getValue(String businessType, String businessNo);

    /**
     * 构建幂等 Key
     *
     * @param businessType 业务类型
     * @param businessNo   业务单号
     * @return 幂等 Key（格式：stock:event:{businessType}:{businessNo}）
     */
    String buildKey(String businessType, String businessNo);
}
