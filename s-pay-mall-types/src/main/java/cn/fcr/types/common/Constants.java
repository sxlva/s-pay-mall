package cn.fcr.types.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class Constants {

    public final static String SPLIT = ",";

    public static final String REDIS_WECHAT_BIND_TICKET_PREFIX = "wechat:bind:ticket:";
    public static final String REDIS_WECHAT_ACCESS_TOKEN_PREFIX = "wechat:access_token:";
    public static final String REDIS_USER_REGISTER_LOCK_PREFIX = "user:register:lock:";
    public static final String REDIS_BIND_STATUS_PENDING = "BINDING_PENDING";

    public static final String IDENTITY_TYPE_WECHAT_MP = "WECHAT_MP";
    public static final String DEFAULT_ROLE_MEMBER = "MEMBER";
    public static final Integer USER_STATUS_ACTIVE = 1;
    public static final Integer USER_STATUS_WECHAT = 2;
    public static final Integer USER_STATUS_DISABLED = 0;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public enum ResponseCode {
        SUCCESS("0000", "调用成功"),
        UN_ERROR("0001", "调用失败"),
        ILLEGAL_PARAMETER("0002", "非法参数"),
        NO_LOGIN("0003", "未登录"),
        BANNED("0403", "账号已被封禁"),
        ;

        private String code;
        private String info;

    }

}
