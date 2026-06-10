package cn.fcr.api.dto;

import lombok.Data;

/**
 * 用户登录请求DTO
 * <p>
 * 用于用户登录的请求参数封装
 */
@Data
public class UserLoginRequestDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}