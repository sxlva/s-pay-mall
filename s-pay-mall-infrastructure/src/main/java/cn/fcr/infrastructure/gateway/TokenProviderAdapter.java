package cn.fcr.infrastructure.gateway;

import cn.fcr.domain.auth.gateway.ITokenProvider;
import cn.fcr.infrastructure.config.JwtTokenProvider;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class TokenProviderAdapter implements ITokenProvider {

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public String createToken(Long userId, String username, String role) {
        return jwtTokenProvider.createToken(userId, username, role);
    }
}
