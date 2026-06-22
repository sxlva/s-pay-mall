package cn.fcr.domain.auth.gateway;

/**
 * Token 提供者接口，定义 JWT Token 生成的抽象。
 *
 * @author 傅崇睿
 */
public interface ITokenProvider {

    /**
     * 创建 JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色编码
     * @return JWT Token 字符串
     */
    String createToken(Long userId, String username, String role);
}
