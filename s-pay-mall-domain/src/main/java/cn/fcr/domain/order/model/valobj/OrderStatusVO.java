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
    PAY_WAIT("PAY_WAIT", "等待支付 - 订单创建完成后，创建支付单"),
    /** 支付成功 —— 接收到支付回调消息 */
    PAY_SUCCESS("PAY_SUCCESS", "支付成功 - 接收到支付回调消息"),
    /** 交易完成 —— 商品发货完成 */
    DEAL_DONE("DEAL_DONE", "交易完成 - 商品发货完成"),
    /** 超时关单 —— 超时未支付 */
    CLOSE("CLOSE", "超时关单 - 超市未支付"),
    ;

    private final String code;
    private final String desc;

}
