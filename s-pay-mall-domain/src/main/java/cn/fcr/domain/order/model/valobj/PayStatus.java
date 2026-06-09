package cn.fcr.domain.order.model.valobj;

public enum PayStatus {
    WAIT_PAY("WAIT_PAY", "待支付"),
    PAYING("PAYING", "支付中"),
    PAID("PAID", "已支付"),
    TRADE_DONE("TRADE_DONE", "交易完成"),
    CLOSED("CLOSED", "已关闭"),
    FAILED("FAILED", "支付失败");

    private final String code;
    private final String description;

    PayStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PayStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (PayStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}