package cn.fcr.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户注册请求DTO
 * <p>
 * 用于用户注册的请求参数封装，支持普通注册和微信注册
 */
@Data
public class UserRegisterRequestDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 微信OpenID，微信注册时必填
     */
    private String openId;
}