package cn.fcr.domain.order.model.vo;

/**
 * 支付状态枚举，归属 order 领域（2026-10-03 M2 重组后由 shared 收编），
 * pay_order 表 status 列与本枚举 code 一一对应。
 *
 * @author 傅崇睿
 */
public enum PayStatus {
    /** 待支付 */
    WAIT_PAY("WAIT_PAY", "待支付"),
    /** 支付中 */
    PAYING("PAYING", "支付中"),
    /** 已支付 */
    PAID("PAID", "已支付"),
    /** 交易完成 */
    TRADE_DONE("TRADE_DONE", "交易完成"),
    /** 已关闭 */
    CLOSED("CLOSED", "已关闭"),
    /** 支付失败 */
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
