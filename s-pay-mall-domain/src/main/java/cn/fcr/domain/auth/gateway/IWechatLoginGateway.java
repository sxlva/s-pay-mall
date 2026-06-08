package cn.fcr.domain.auth.gateway;

public interface IWechatLoginGateway {

    Long findUserIdByOpenid(String openid);

    Long createWechatUserAndBind(String openid);

    void saveLoginToken(String ticket, String token);

    String getLoginToken(String ticket);
}
