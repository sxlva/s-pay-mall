package cn.fcr.domain.mall.gateway;

/**
 * 用户绑定网关接口
 * 提供用户与第三方平台绑定关系的查询能力
 */
public interface IUserBindingGateway {

    /**
     * 根据用户ID获取微信 openid
     *
     * @param userId 用户ID
     * @return 微信 openid，若未绑定返回 null
     */
    String getWeChatOpenIdByUserId(Long userId);

    /**
     * 检查微信 openid 是否已被绑定
     *
     * @param openId 微信 openid
     * @return 已绑定返回 true，未绑定返回 false
     */
    boolean isWeChatOpenIdBound(String openId);

    /**
     * 绑定微信 openid 到用户
     *
     * @param userId 用户ID
     * @param openId 微信 openid
     */
    void bindWeChatOpenId(Long userId, String openId);
}