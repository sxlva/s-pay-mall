package cn.fcr.api.dto;

import lombok.Data;

/**
 * 用户保存请求DTO
 * <p>
 * 用于创建或更新用户的请求参数封装
 */
@Data
public class UserSaveRequestDTO {

    /**
     * 用户ID，创建时为null，更新时必填
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码，更新时可为空表示不修改密码
     */
    private String password;

    /**
     * 用户状态：0-禁用，1-启用
     */
    private Integer status;
}