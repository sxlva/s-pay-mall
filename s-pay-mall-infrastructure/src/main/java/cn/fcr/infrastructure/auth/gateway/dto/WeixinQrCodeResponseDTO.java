package cn.fcr.infrastructure.auth.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description 微信二维码响应DTO
 *
 * 【职责说明】
 * - 封装微信API创建二维码的响应数据
 * - 包含二维码ticket，用于生成二维码图片URL
 *
 * 【字段说明】
 * - ticket: 二维码ticket，可用于换取二维码图片
 * - expire_seconds: 二维码过期时间（秒）
 * - url: 二维码图片解析后的URL
 * - errcode: 错误码（成功时为空）
 * - errmsg: 错误信息（成功时为空）
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
