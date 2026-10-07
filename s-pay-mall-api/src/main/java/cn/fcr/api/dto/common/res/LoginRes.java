package cn.fcr.api.dto.common.res;

import lombok.Data;

/**
 * 用户登录信息视图对象
 *
 * @author 傅崇睿
 */
@Data
public class LoginRes {

    /** JWT Token */
    private String token;

    /** 用户ID（2026-10-07 起 camelCase 输出，移除历史遗留 @JsonProperty("user_id")，关闭 TD-12） */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 角色 */
    private String role;
}
