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
        log.info("处理微信扫码登录: ticket={}, openid={}", ticket, openid);

        try {
            Long userId = wechatLoginGateway.findUserIdByOpenid(openid);

            String token;
            if (userId == null) {
                log.info("微信用户首次登录，开始自动注册: openid={}", openid);
                UserLoginVO loginVO = mallUserService.registerWeChatUserByScan(openid);
                userId = loginVO.getUserId();
                token = loginVO.getToken();
                log.info("自动注册成功: userId={}", userId);
            } else {
                log.info("微信用户已绑定，查询到用户: userId={}", userId);
                token = authTokenGateway.createToken(userId, Constants.WX_USER_USERNAME_PREFIX + userId, Constants.DEFAULT_ROLE_MEMBER);
            }

            log.info("生成JWT Token成功: userId={}", userId);

            wechatLoginGateway.saveLoginToken(ticket, token);
            sendLoginNotificationBestEffort(ticket, openid);

            return token;

        } catch (RuntimeException e) {
            // 业务异常与网关异常原样上抛，保持 Application 层事务回滚语义与异常类型不变
            log.error("微信扫码登录处理失败: ticket={}, openid={}", ticket, openid, e);
            throw e;
        } catch (Exception e) {
            // 仅包装意外的受检异常（当前链路均为 RuntimeException，此分支为防御性保留）
            log.error("微信扫码登录处理失败: ticket={}, openid={}", ticket, openid, e);
            throw new RuntimeException("微信登录处理失败", e);
        }
    }

    /**
     * 发送微信登录通知（best-effort）
     *
     * <p>【P0-3 业务验收语义】Redis 登录态已先行存入，微信模板消息只是扫码人
     * 微信侧的辅助提醒——前端经 check_login 轮询取 token，不依赖本条消息。
     * 因此通知失败只记录日志（带异常），不影响已完成的自动注册与登录，
     * 也不得触发事务回滚。</p>
     *
     * @param ticket 票据（仅用于日志）
     * @param openid 接收通知的微信 openid
     */
    private void sendLoginNotificationBestEffort(String ticket, String openid) {
        try {
            weChatGateway.sendLoginNotification(openid);
        } catch (Exception e) {
            log.error("发送微信登录通知失败（不影响登录结果）: ticket={}, openid={}", ticket, openid, e);
        }
    }
}
