package cn.fcr.domain.order.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态值对象
 *
 * @author 傅崇睿
 */
@Getter
@AllArgsConstructor
public enum OrderStatusVO {

    /** 创建完成 —— 如果调单了，也会从创建记录重新发起创建支付单 */
    CREATE("CREATE", "创建完成 - 如果调单了，也会从创建记录重新发起创建支付单"),
    /** 等待支付 —— 订单创建完成后，创建支付单 */
    PAY_WAIT("WAIT_PAY", "等待支付 - 订单创建完成后，创建支付单"),
    /** 支付成功 —— 接收到支付回调消息 */
    PAY_SUCCESS("PAY_SUCCESS", "支付成功 - 接收到支付回调消息"),
    /** 交易完成 —— 商品发货完成 */
    DEAL_DONE("DEAL_DONE", "交易完成 - 商品发货完成"),
    /** 超时关单 —— 超时未支付 */
    CLOSE("CLOSED", "超时关单 - 超时未支付"),
    ;

    private final String code;
    private final String desc;

    /**
     * 根据数据库状态码获取对应的枚举值
     *
     * @param code 数据库状态码
     * @return 对应的枚举值，若未匹配返回 null
     */
    public static OrderStatusVO fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (OrderStatusVO status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

}
