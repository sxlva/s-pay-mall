package cn.fcr.domain.auth.service;

import cn.fcr.domain.auth.repository.IWeChatTokenRepository;

import java.util.logging.Logger;

/**
 * 微信绑定服务
 *
 * <p>负责处理微信公众号绑定的状态管理
 * 通过 IWeChatTokenRepository 抽象接口操作缓存
 */
public class WeixinBindService {

    private final Logger logger = Logger.getLogger(WeixinBindService.class.getName());

    private final IWeChatTokenRepository weChatTokenRepository;

    public WeixinBindService(IWeChatTokenRepository weChatTokenRepository) {
        this.weChatTokenRepository = weChatTokenRepository;
    }

    /**
     * 初始化绑定状态
     *
     * @param ticket 绑定票据（UUID）
     */
    public void initBindStatus(String ticket) {
        weChatTokenRepository.initBindStatus(ticket);
        logger.info("初始化微信绑定状态 ticket:" + ticket);
    }

    /**
     * 更新绑定状态，写入 OpenID
     *
     * @param ticket 绑定票据
     * @param openId 用户的微信 OpenID
     */
    public void updateBindStatus(String ticket, String openId) {
        weChatTokenRepository.saveBindTicket(ticket, openId);
        logger.info("更新微信绑定状态 ticket:" + ticket + " openId:" + openId);
    }

    /**
     * 查询绑定状态
     *
     * @param ticket 绑定票据
     * @return OpenID，如果未绑定返回 null
     */
    public String checkBindStatus(String ticket) {
        return weChatTokenRepository.getOpenIdByTicket(ticket);
    }

    /**
     * 获取原始绑定状态值（包括 PENDING 状态）
     *
     * @param ticket 绑定票据
     * @return 状态值
     */
    public String getBindStatusRaw(String ticket) {
        return weChatTokenRepository.getBindStatusRaw(ticket);
    }

    /**
     * 注册防重入锁
     *
     * @param username 用户名
     * @return true 表示获取锁成功，false 表示已存在锁
     */
    public boolean tryAcquireRegisterLock(String username) {
        return weChatTokenRepository.tryAcquireRegisterLock(username);
    }

    /**
     * 释放注册锁
     *
     * @param username 用户名
     */
    public void releaseRegisterLock(String username) {
        weChatTokenRepository.releaseRegisterLock(username);
    }
}