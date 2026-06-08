package cn.fcr.domain.mall.gateway;

public interface IAuthTokenGateway {

    String createToken(Long userId, String username, String role);

    String encodePassword(String rawPassword);

    boolean matchesPassword(String rawPassword, String encodedPassword);
}