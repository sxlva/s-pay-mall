package cn.fcr.types.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class Constants {

    public final static String SPLIT = ",";
    public final static String COLON = ":";
    public final static String SPACE = " ";
    public final static String UNDERLINE = "_";

    public static class RedisKey {
        public static  String WEIXIN_ACCESS_TOKEN_KEY_PREFIX = "weixin_access_token_";
        public static  String WEIXIN_LOGIN_STATE_KEY_PREFIX = "weixin_login_state_";
        // access_token 过期时间 2小时
        public static  long ACCESS_TOKEN_EXPIRE_TIME = 7200 * 1000;
        // 登录状态过期时间 5分钟
        public static  long LOGIN_STATE_EXPIRE_TIME = 5 * 60 * 1000;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public enum ResponseCode {
        SUCCESS("0000", "调用成功"),
        UN_ERROR("0001", "调用失败"),
        ILLEGAL_PARAMETER("0002", "非法参数"),
        NO_LOGIN("0003", "未登录"),
        ;

        private String code;
        private String info;

    }

    @Getter
    @AllArgsConstructor
    public enum OrderStatusEnum {

        CREATE("CREATE", "创建完成 - 如果调单了，也会从创建记录重新发起创建支付单"),
        PAY_WAIT("PAY_WAIT", "等待支付 - 订单创建完成后，创建支付单"),
        PAY_SUCCESS("PAY_SUCCESS", "支付成功 - 接收到支付回调消息"),
        DEAL_DONE("DEAL_DONE", "交易完成 - 商品发货完成"),
        CLOSE("CLOSE", "超时关单 - 超市未支付"),
        ;

        private final String code;
        private final String desc;

    }

}
