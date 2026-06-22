package cn.fcr.domain.auth.repository;

/**
 * 微信凭证仓储接口，定义微信绑定状态管理的抽象，由基础设施层实现。
 *
 * @author 傅崇睿
 */
public interface IWeChatTokenRepository {
    
    /**
     * 缓存微信绑定临时凭证，设置 5 分钟过期
     * 
     * @param uuid 唯一标识
     * @param openId 微信 OpenID
     */
    void saveBindTicket(String uuid, String openId);
    
    /**
     * 查询当前 Ticket 绑定的 OpenID
     * 
     * @param uuid 唯一标识
     * @return OpenID，如果不存在返回 null
     */
    String getOpenIdByTicket(String uuid);
    
    /**
     * 初始化绑定状态（设置为待绑定状态）
     * 
     * @param uuid 唯一标识
     */
    void initBindStatus(String uuid);
    
    /**
     * 获取原始绑定状态值（包括待绑定状态）
     * 
     * @param uuid 唯一标识
     * @return 状态值
     */
    String getBindStatusRaw(String uuid);
    
    /**
     * 获取注册防重入锁
     * 
     * @param username 用户名
     * @return true 表示获取锁成功，false 表示已存在锁
     */
    boolean tryAcquireRegisterLock(String username);
    
    /**
     * 释放注册锁
     * 
     * @param username 用户名
     */
    void releaseRegisterLock(String username);
}