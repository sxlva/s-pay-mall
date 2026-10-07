package cn.fcr.domain.order.model.entity;

/**
 * 订单状态枚举，封装领域状态码、中文描述与数据库存储状态的映射。
 *
 * <p>【状态口径】领域与接口层统一使用 {@link #code}（INIT/PAID/SHIPPED/DONE/CANCELED）；
 * DB 存储为混合口径（历史实现与 E2E 验收固化，不可单方面改动）：INIT 存 CREATED（{@link #toDbStatus()}），
 * 其余状态按 code 原样存储；读取统一经 {@link #fromDbStatus(String)}（内含 code 兜底）双向兼容。
 * 存量 DONE 订单中可能存在历史写入的 COMPLETED 值，fromDbStatus 显式兼容该旧值。</p>
 *
 * @author 傅崇睿
 */
public enum OrderState {

    /** 待支付 */
    INIT("INIT", "待支付", "CREATED"),
    /** 已支付 */
    PAID("PAID", "已支付", "PAID"),
    /** 已发货 */
    SHIPPED("SHIPPED", "已发货", "SHIPPED"),
    /** 已完成 */
    DONE("DONE", "已完成", "DONE"),
    /** 已取消 */
    CANCELED("CANCELED", "已取消", "CANCELED");

    private final String code;
    private final String description;
    private final String dbStatus;

    OrderState(String code, String description, String dbStatus) {
        this.code = code;
        this.description = description;
        this.dbStatus = dbStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OrderState fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OrderState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        return null;
    }

    /**
     * 获取状态
     *
     * @param code 状态码
     * @return 中文描述，若无法识别则返回原 code
     */
    public static String describe(String code) {
        OrderState state = fromCode(code);
        return state != null ? state.getDescription() : code;
    }

    /**
     * 将领域状态转换为数据库存储状态
     * 【DDD 原则】状态映射逻辑封装在领域对象中，避免泄露到 Infrastructure 层
     *
     * @return 数据库状态字符串
     */
    public String toDbStatus() {
        return this.dbStatus;
    }

    /**
     * 从数据库状态转换为领域状态
     * 【DDD 原则】状态映射逻辑封装在领域对象中，避免泄露到 Infrastructure 层
     *
     * @param dbStatus 数据库状态字符串
     * @return 领域状态枚举，若无法识别则返回 null
     */
    public static OrderState fromDbStatus(String dbStatus) {
        if (dbStatus == null) {
            return null;
        }
        // 兼容存量数据：历史版本 DONE 曾写入 COMPLETED、CANCELED 曾写入 CANCELLED（TD-4 修复后新数据统一写 code）
        if ("COMPLETED".equals(dbStatus)) {
            return DONE;
        }
        if ("CANCELLED".equals(dbStatus)) {
            return CANCELED;
        }
        for (OrderState state : values()) {
            if (state.dbStatus.equals(dbStatus)) {
                return state;
            }
        }
        return fromCode(dbStatus);
    }
}