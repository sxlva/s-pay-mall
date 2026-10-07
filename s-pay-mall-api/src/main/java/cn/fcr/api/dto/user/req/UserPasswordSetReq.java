package cn.fcr.api.dto.user.req;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 用户密码设置请求DTO
 * <p>
 * 微信扫码注册的用户（无账密）通过本请求设置账户密码，
 * 设置后该账户同时支持微信扫码与账密两种登录方式。
 *
 * @author 傅崇睿
 */
@Data
public class UserPasswordSetReq {

    /**
     * 明文密码（服务端 BCrypt 加密存储）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6~64 位之间")
    private String password;
}
