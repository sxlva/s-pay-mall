package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 用户登录信息视图对象
 */
@Data
public class UserLoginVO {
    private String token;

    @JsonProperty("user_id")
    private Long userId;

    private String username;
    private String role;
}
