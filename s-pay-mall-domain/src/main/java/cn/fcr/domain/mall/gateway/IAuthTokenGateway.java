package cn.fcr.domain.mall.gateway;

/**
 * 认证令牌网关接口，定义 JWT Token 生成和密码编码的抽象。
 *
 * @author 傅崇睿
 */
public interface IAuthTokenGateway {

    String createToken(Long userId, String username, String role);

    String encodePassword(String rawPassword);

    boolean matchesPassword(String rawPassword, String encodedPassword);
}