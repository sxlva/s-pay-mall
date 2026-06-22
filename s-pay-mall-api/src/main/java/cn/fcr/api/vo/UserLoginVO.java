package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 用户登录信息视图对象
 *
 * @author 傅崇睿
 */
@Data
public class UserLoginVO {

    /** JWT Token */
    private String token;

    /** 用户ID */
    @JsonProperty("user_id")
    private Long userId;

    /** 用户名 */
    private String username;

    /** 角色 */
    private String role;
}
