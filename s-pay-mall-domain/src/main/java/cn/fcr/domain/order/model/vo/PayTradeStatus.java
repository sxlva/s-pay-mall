package cn.fcr.domain.order.model.vo;

/**
 * 支付宝交易状态枚举，承载支付宝回调 trade_status 字段的语义。
 *
 * <p>【P0-6】"哪些交易状态视为支付成功"的规则唯一收敛在本枚举的
 * {@link #isSuccess()} 中，Trigger 层不再出现 trade_status 字符串硬编码判断。</p>
 *
 * @author 傅崇睿
 */
public enum PayTradeStatus {
    /** 交易创建，等待买家付款 */
    WAIT_BUYER_PAY("WAIT_BUYER_PAY", "等待买家付款", false),
    /** 交易关闭（未付款超时关单、全额退款等） */
    TRADE_CLOSED("TRADE_CLOSED", "交易关闭", false),
    /** 交易支付成功 */
    TRADE_SUCCESS("TRADE_SUCCESS", "支付成功", true),
    /** 交易结束（不可退款），与支付成功同等对待 */
    TRADE_FINISHED("TRADE_FINISHED", "交易完成", true);

    private final String code;
    private final String description;
    /** 是否视为支付成功（触发履约） */
    private final boolean success;

    PayTradeStatus(String code, String description, boolean success) {
        this.code = code;
        this.description = description;
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 是否视为支付成功
     *
     * @return true 表示应触发订单履约（TRADE_SUCCESS / TRADE_FINISHED）
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 按支付宝回调值解析交易状态
     *
     * @param code 支付宝 trade_status 原始值
     * @return 对应枚举，无法识别时返回 null
     */
    public static PayTradeStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (PayTradeStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
