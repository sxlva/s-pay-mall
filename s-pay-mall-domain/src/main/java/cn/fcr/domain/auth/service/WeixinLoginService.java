package cn.fcr.domain.auth.service;

import cn.fcr.domain.auth.gateway.IWeChatGateway;
import cn.fcr.domain.auth.gateway.IWechatLoginGateway;
import cn.fcr.domain.auth.gateway.ITokenProvider;
import cn.fcr.types.common.Constants;

import java.util.logging.Logger;

public class WeixinLoginService implements ILoginService {

    private final Logger logger = Logger.getLogger(WeixinLoginService.class.getName());

    private final IWeChatGateway weChatGateway;
    private final IWechatLoginGateway wechatLoginGateway;
    private final ITokenProvider tokenProvider;

    public WeixinLoginService(IWeChatGateway weChatGateway,
                              IWechatLoginGateway wechatLoginGateway,
                              ITokenProvider tokenProvider) {
        this.weChatGateway = weChatGateway;
        this.wechatLoginGateway = wechatLoginGateway;
        this.tokenProvider = tokenProvider;
    }

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
        wechatLoginGateway.saveLoginToken(ticket, openid);
        weChatGateway.sendLoginNotification(openid);
    }

    @Override
    public String handleWechatScanLogin(String ticket, String openid) {
        logger.info("处理微信扫码登录: ticket=" + ticket + ", openid=" + openid);

        try {
            Long userId = wechatLoginGateway.findUserIdByOpenid(openid);

            if (userId == null) {
                logger.info("微信用户首次登录，开始自动注册: openid=" + openid);
                userId = wechatLoginGateway.createWechatUserAndBind(openid);
                logger.info("自动注册成功: userId=" + userId);
            } else {
                logger.info("微信用户已绑定，查询到用户: userId=" + userId);
            }

            String username = "wx_user_" + userId;
            String token = tokenProvider.createToken(userId, username, Constants.DEFAULT_ROLE_MEMBER);
            logger.info("生成JWT Token成功: userId=" + userId);

            wechatLoginGateway.saveLoginToken(ticket, token);
            weChatGateway.sendLoginNotification(openid);

            return token;

        } catch (Exception e) {
            logger.severe("微信扫码登录处理失败: ticket=" + ticket + ", openid=" + openid);
            throw new RuntimeException("微信登录处理失败", e);
        }
    }
}
