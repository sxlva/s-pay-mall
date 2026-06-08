package cn.fcr.domain.auth.gateway;

public interface ITokenProvider {

    String createToken(Long userId, String username, String role);
}
