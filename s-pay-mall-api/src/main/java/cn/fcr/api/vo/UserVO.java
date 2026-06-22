package cn.fcr.api.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象
 *
 * @author 傅崇睿
 */
@Data
public class UserVO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 用户状态：0-禁用，1-启用 */
    private Integer status;

    /** 角色编码 */
    @JsonProperty("role_code")
    private String roleCode;

    /** 角色名称 */
    @JsonProperty("role_name")
    private String roleName;

    /** 创建时间 */
    @JsonProperty("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonProperty("update_time")
    private LocalDateTime updateTime;
}
