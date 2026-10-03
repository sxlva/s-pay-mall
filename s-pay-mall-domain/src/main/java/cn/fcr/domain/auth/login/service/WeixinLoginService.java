package cn.fcr.domain.auth.login.service;

import cn.fcr.domain.auth.token.gateway.IAuthTokenGateway;
import cn.fcr.domain.auth.login.gateway.IWeChatGateway;
import cn.fcr.domain.auth.login.gateway.IWechatLoginGateway;
import cn.fcr.domain.mall.user.model.valobj.UserLoginVO;
import cn.fcr.domain.mall.user.service.IMallUserService;
import cn.fcr.types.common.Constants;

import lombok.extern.slf4j.Slf4j;

/**
 * 微信扫码登录服务实现，负责创建二维码票据、检查登录状态、
 * 保存登录状态以及处理微信扫码登录（自动注册与绑定）。
 *
 * <p>【P0-6】首次扫码的自动注册已收敛到 {@link IMallUserService#registerWeChatUserByScan}，
 * 本类不再经网关直写 DAO 建户，"微信建户"规则在 domain 层唯一。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
public class WeixinLoginService implements ILoginService {

    private final IWeChatGateway weChatGateway;
    private final IWechatLoginGateway wechatLoginGateway;
    private final IAuthTokenGateway authTokenGateway;
    private final IMallUserService mallUserService;

    public WeixinLoginService(IWeChatGateway weChatGateway,
                              IWechatLoginGateway wechatLoginGateway,
                              IAuthTokenGateway authTokenGateway,
                              IMallUserService mallUserService) {
        this.weChatGateway = weChatGateway;
        this.wechatLoginGateway = wechatLoginGateway;
        this.authTokenGateway = authTokenGateway;
        this.mallUserService = mallUserService;
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
        log.info("处理微信扫码登录: ticket=" + ticket + ", openid=" + openid);

        try {
            Long userId = wechatLoginGateway.findUserIdByOpenid(openid);

            String token;
            if (userId == null) {
                log.info("微信用户首次登录，开始自动注册: openid=" + openid);
                UserLoginVO loginVO = mallUserService.registerWeChatUserByScan(openid);
                userId = loginVO.getUserId();
                token = loginVO.getToken();
                log.info("自动注册成功: userId=" + userId);
            } else {
                log.info("微信用户已绑定，查询到用户: userId=" + userId);
                token = authTokenGateway.createToken(userId, "wx_user_" + userId, Constants.DEFAULT_ROLE_MEMBER);
            }

            log.info("生成JWT Token成功: userId=" + userId);

            wechatLoginGateway.saveLoginToken(ticket, token);
            weChatGateway.sendLoginNotification(openid);

            return token;

        } catch (Exception e) {
            log.error("微信扫码登录处理失败: ticket=" + ticket + ", openid=" + openid);
            throw new RuntimeException("微信登录处理失败", e);
        }
    }
}
