package cn.fcr.api.dto.user.req;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信绑定确认请求DTO
 * <p>
 * 账密登录用户扫码后，凭绑定二维码票据将微信 OpenID 绑定到当前登录账号。
 * OpenID 由服务端按 ticket 从缓存解析，不回传前端（见 ROADMAP TD-10）。
 *
 * @author 傅崇睿
 */
@Data
public class UserBindConfirmReq {

    /**
     * 绑定二维码票据（/auth/bind/qrcode 返回）
     */
    @NotBlank(message = "绑定票据不能为空")
    private String ticket;
}
