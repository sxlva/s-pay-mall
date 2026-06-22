package cn.fcr.infrastructure.auth.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信创建二维码响应DTO
 *
 * @author 傅崇睿
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeixinQrCodeResponseDTO {

    /**
     * 二维码ticket，用于换取二维码图片
     */
    private String ticket;

    /**
     * 二维码过期时间（秒）
     */
    private Integer expire_seconds;

    /**
     * 二维码图片解析后的URL
     */
    private String url;

    /**
     * 错误码
     */
    private Integer errcode;

    /**
     * 错误信息
     */
    private String errmsg;

}
