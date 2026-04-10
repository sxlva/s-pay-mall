package cn.fcr.domain.auth.service;

import cn.fcr.domain.auth.adapter.port.ILoginPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author 傅崇睿
 * @date 2025/7/26 14:58
 * @description 实现登陆服务
 */
@Slf4j
@Service
public class WeixinLoginService implements ILoginService {

    @Resource
    private ILoginPort loginPort;

    @Override
    public String createQrCodeTicket() throws Exception {
        return loginPort.createQrCodeTicket();
    }

    @Override
    public String checkLogin(String ticket) {
        return loginPort.checkLogin(ticket);
    }

    @Override
    public void saveLoginState(String ticket, String openid) throws IOException {
        // 保存登录信息
        loginPort.saveLoginState(ticket, openid);
        // 发送模板消息
        loginPort.sendLoginTemplate(openid);
    }
}
