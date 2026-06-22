package cn.fcr.domain.mall.model.entity;

/**
 * 订单状态枚举，封装领域状态码、中文描述与数据库存储状态的映射。
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
    DONE("DONE", "已完成", "COMPLETED"),
    /** 已取消 */
    CANCELED("CANCELED", "已取消", "CANCELLED");

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
        for (OrderState state : values()) {
            if (state.dbStatus.equals(dbStatus)) {
                return state;
            }
        }
        return fromCode(dbStatus);
    }
}