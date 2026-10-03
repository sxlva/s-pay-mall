package cn.fcr.infrastructure.auth.token;

import cn.fcr.domain.auth.token.gateway.IAuthTokenGateway;
import cn.fcr.infrastructure.auth.token.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 认证Token网关实现
 *
 * @author 傅崇睿
 */
@Component
public class AuthTokenGatewayImpl implements IAuthTokenGateway {

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public String createToken(Long userId, String username, String role) {
        return jwtTokenProvider.createToken(userId, username, role);
    }

    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
