package cn.fcr.domain.auth.adapter.port;

import java.io.IOException;

/**
 * @author xiaolv
 * @date 2025/7/26 15:07
 * @description 定义微信登陆外部接口实现标准
 */
public interface ILoginPort {
    String createQrCodeTicket() throws IOException;

    void sendLoginTemplate(String openid) throws IOException;
}
