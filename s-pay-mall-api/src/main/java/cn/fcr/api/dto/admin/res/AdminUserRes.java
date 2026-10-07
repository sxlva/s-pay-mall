package cn.fcr.api.dto.admin.res;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象
 *
 * @author 傅崇睿
 */
@Data
public class AdminUserRes {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 用户状态：0-禁用，1-启用 */
    private Integer status;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
