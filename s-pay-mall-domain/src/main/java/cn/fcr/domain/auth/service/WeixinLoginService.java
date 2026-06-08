package cn.fcr.domain.auth.service;

import cn.fcr.domain.auth.gateway.IWeChatGateway;
import cn.fcr.domain.auth.gateway.IWechatLoginGateway;
import cn.fcr.domain.auth.gateway.ITokenProvider;
import cn.fcr.types.common.Constants;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class WeixinLoginService implements ILoginService {

    @Resource
    private IWeChatGateway weChatGateway;

    @Qualifier("openidToken")
    @Resource
    private Cache<String, String> openidToken;

    @Resource
    private IWechatLoginGateway wechatLoginGateway;

    @Resource
    private ITokenProvider tokenProvider;

    @Override
    public String createQrCodeTicket() {
        return weChatGateway.createQrCodeTicket();
    }

    @Override
    public String checkLogin(String ticket) {
        return wechatLoginGateway.getLoginToken(ticket);
    }

    @Override
    public void saveLoginState(String ticket, String openid) {
        openidToken.put(ticket, openid);
        weChatGateway.sendLoginNotification(openid);
    }

    @Override
    public String handleWechatScanLogin(String ticket, String openid) {
        log.info("处理微信扫码登录: ticket={}, openid={}", ticket, openid);
        
        try {
            Long userId = wechatLoginGateway.findUserIdByOpenid(openid);
            
            if (userId == null) {
                log.info("微信用户首次登录，开始自动注册: openid={}", openid);
                userId = wechatLoginGateway.createWechatUserAndBind(openid);
                log.info("自动注册成功: userId={}", userId);
            } else {
                log.info("微信用户已绑定，查询到用户: userId={}", userId);
            }
            
            String username = "wx_user_" + userId;
            String token = tokenProvider.createToken(userId, username, Constants.DEFAULT_ROLE_MEMBER);
            log.info("生成JWT Token成功: userId={}", userId);
            
            wechatLoginGateway.saveLoginToken(ticket, token);
            weChatGateway.sendLoginNotification(openid);
            
            return token;
            
        } catch (Exception e) {
            log.error("微信扫码登录处理失败: ticket={}, openid={}", ticket, openid, e);
            throw new RuntimeException("微信登录处理失败", e);
        }
    }
}
