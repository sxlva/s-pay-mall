package cn.fcr.domain.auth.service;

import java.io.IOException;

/**
 * @author 傅崇睿
 * @date 2025/7/26 14:57
 * @description 登陆服务接口
 */
public interface ILoginService {

    String createQrCodeTicket() throws Exception;

    String checkLogin(String ticket);

    void saveLoginState(String ticket, String openid) throws IOException;

}
