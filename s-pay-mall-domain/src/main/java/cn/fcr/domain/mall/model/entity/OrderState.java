package cn.fcr.domain.mall.model.entity;

public enum OrderState {

    INIT("INIT", "待支付"),

    PAID("PAID", "已支付"),

    SHIPPED("SHIPPED", "已发货"),

    DONE("DONE", "已完成"),

    CANCELED("CANCELED", "已取消");

    private final String code;
    private final String description;

    OrderState(String code, String description) {
        this.code = code;
        this.description = description;
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
     * 将领域状态转换为数据库存储状态
     * 【DDD 原则】状态映射逻辑封装在领域对象中，避免泄露到 Infrastructure 层
     *
     * @return 数据库状态字符串
     */
    public String toDbStatus() {
        switch (this) {
            case INIT:
                return "CREATED";
            case PAID:
                return "PAID";
            case SHIPPED:
                return "SHIPPED";
            case DONE:
                return "COMPLETED";
            case CANCELED:
                return "CANCELLED";
            default:
                return this.code;
        }
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
        switch (dbStatus) {
            case "CREATED":
                return INIT;
            case "PAID":
                return PAID;
            case "SHIPPED":
                return SHIPPED;
            case "COMPLETED":
                return DONE;
            case "CANCELLED":
                return CANCELED;
            default:
                // 尝试使用 fromCode 兼容直接存储枚举 code 的情况
                return fromCode(dbStatus);
        }
    }
}
