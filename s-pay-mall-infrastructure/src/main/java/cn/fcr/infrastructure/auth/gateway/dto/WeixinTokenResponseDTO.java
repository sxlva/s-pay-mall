package cn.fcr.infrastructure.auth.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信Access Token响应DTO
 *
 * @author 傅崇睿
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
