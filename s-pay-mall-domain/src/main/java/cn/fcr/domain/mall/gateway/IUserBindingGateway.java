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
}