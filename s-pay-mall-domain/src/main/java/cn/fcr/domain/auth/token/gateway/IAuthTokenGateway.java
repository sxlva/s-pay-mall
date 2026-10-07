package cn.fcr.domain.auth.token.gateway;

/**
 * 认证令牌网关接口，定义 JWT Token 生成和密码编码的抽象。
 *
 * @author 傅崇睿
 */
public interface IAuthTokenGateway {

    /**
     * 生成 JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色编码
     * @return 签发后的 JWT Token 字符串
     */
    String createToken(Long userId, String username, String role);

    /**
     * 明文密码加密（BCrypt）
     *
     * @param rawPassword 明文密码
     * @return 加密后的密码密文
     */
    String encodePassword(String rawPassword);

    /**
     * 校验明文密码与密文是否匹配（BCrypt）
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 已加密的密码密文
     * @return true=匹配，false=不匹配
     */
    boolean matchesPassword(String rawPassword, String encodedPassword);
}