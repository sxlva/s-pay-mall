package cn.fcr.application;

import cn.fcr.domain.auth.login.service.ILoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证应用层服务
 *
 * <p>承担认证领域用例的事务边界。微信扫码登录的"首次自动注册"
 * （创建用户 → 更新用户名 → 绑定微信 → 分配角色，4 个写操作）由本类
 * 的 {@link #handleWechatScanLogin} 事务统一保护；新用户判断等业务规则
 * 仍在 Domain 层 {@link ILoginService} 内，本类仅做编排不重写。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@Service
public class AuthApplicationService {

    /** 登录领域服务（微信扫码登录业务规则，含新用户自动注册判断） */
    private final ILoginService loginService;

    public AuthApplicationService(ILoginService loginService) {
        this.loginService = loginService;
    }

    /**
     * 处理微信扫码登录（含首次自动注册）
     *
     * <p>【P0-3】事务边界在 Application 层：openid 未绑定时 Domain 服务
     * 自动注册的 4 个写操作在同一事务内完成，中途异常整体回滚；
     * Infrastructure 层不再持有事务注解。</p>
     *
     * @param ticket 二维码票据
     * @param openid 微信用户openid
     * @return JWT token
     */
    @Transactional(rollbackFor = Exception.class)
    public String handleWechatScanLogin(String ticket, String openid) {
        return loginService.handleWechatScanLogin(ticket, openid);
    }
}
