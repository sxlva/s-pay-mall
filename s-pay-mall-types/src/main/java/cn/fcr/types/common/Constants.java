package cn.fcr.types.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 全局常量，包含分隔符、Redis Key 前缀、用户状态等
 *
 * @author 傅崇睿
 */
public class Constants {

    /** 通用分隔符 */
    public final static String SPLIT = ",";

    /** Redis Key 前缀：微信绑定票据 */
    public static final String REDIS_WECHAT_BIND_TICKET_PREFIX = "wechat:bind:ticket:";
    /** Redis Key 前缀：微信 Access Token */
    public static final String REDIS_WECHAT_ACCESS_TOKEN_PREFIX = "wechat:access_token:";
    /** Redis Key 前缀：用户注册锁 */
    public static final String REDIS_USER_REGISTER_LOCK_PREFIX = "user:register:lock:";
    /** 绑定状态：等待绑定 */
    public static final String REDIS_BIND_STATUS_PENDING = "BINDING_PENDING";

    /** 身份类型：微信公众号 */
    public static final String IDENTITY_TYPE_WECHAT_MP = "WECHAT_MP";
    /** 默认角色：会员 */
    public static final String DEFAULT_ROLE_MEMBER = "MEMBER";
    /** 用户状态：正常 */
    public static final Integer USER_STATUS_ACTIVE = 1;
    /** 用户状态：已绑定微信 */
    public static final Integer USER_STATUS_WECHAT = 2;
    /** 用户状态：已禁用 */
    public static final Integer USER_STATUS_DISABLED = 0;

    /** 通用响应码枚举 */
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

        /** 响应码 */
        private String code;
        /** 响应信息 */
        private String info;

    }

}
