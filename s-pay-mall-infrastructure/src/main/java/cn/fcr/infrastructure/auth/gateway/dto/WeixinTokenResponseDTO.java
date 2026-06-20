package cn.fcr.infrastructure.auth.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description 微信 Access Token 响应DTO
 *
 * 【职责说明】
 * - 封装微信API获取Access Token的响应数据
 * - 用于解析微信服务器返回的token信息
 *
 * 【字段说明】
 * - access_token: 访问令牌
 * - expires_in: 过期时间（秒）
 * - errcode: 错误码（成功时为空）
 * - errmsg: 错误信息（成功时为空）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeixinTokenResponseDTO {

    /**
     * 访问令牌
     */
    private String access_token;

    /**
     * 过期时间（秒）
     */
    private Integer expires_in;

    /**
     * 错误码
     */
    private Integer errcode;

    /**
     * 错误信息
     */
    private String errmsg;

}
