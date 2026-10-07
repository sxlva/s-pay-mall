package cn.fcr.api.dto.user.res;

import lombok.Data;

/**
 * 微信绑定状态视图对象
 *
 * @author 傅崇睿
 */
@Data
public class UserBindStatusRes {

    /** 绑定状态 */
    private String status;

    /** 微信 OpenID（2026-10-07 起 camelCase 输出，移除 @JsonProperty("open_id")，TD-1） */
    private String openId;
}
