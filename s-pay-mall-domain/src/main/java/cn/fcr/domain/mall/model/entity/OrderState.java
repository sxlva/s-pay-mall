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
}