package cn.fcr.domain.auth.login.gateway;

/**
 * 微信登录网关接口，定义微信用户查询与登录Token保存的抽象。
 *
 * @author 傅崇睿
 */
public interface IWechatLoginGateway {

    /**
     * 根据 OpenID 查找已绑定的用户ID
     *
     * @param openid 微信 OpenID
     * @return 用户ID，未绑定返回 null
     */
    Long findUserIdByOpenid(String openid);

    /**
     * 保存登录 Token
     *
     * @param ticket 票据
     * @param token  JWT Token
     */
    void saveLoginToken(String ticket, String token);

    /**
     * 获取登录 Token
     *
     * @param ticket 票据
     * @return JWT Token，不存在返回 null
     */
    String getLoginToken(String ticket);
}
