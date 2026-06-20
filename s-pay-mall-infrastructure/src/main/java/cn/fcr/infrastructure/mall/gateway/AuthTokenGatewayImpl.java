package cn.fcr.infrastructure.mall.gateway;

import cn.fcr.domain.mall.gateway.IAuthTokenGateway;
import cn.fcr.infrastructure.config.auth.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

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
