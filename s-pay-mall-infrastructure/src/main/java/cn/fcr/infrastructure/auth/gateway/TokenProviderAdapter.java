package cn.fcr.infrastructure.auth.gateway;

import cn.fcr.domain.auth.gateway.ITokenProvider;
import cn.fcr.infrastructure.config.auth.JwtTokenProvider;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Token提供者适配器（JWT Token）
 *
 * @author 傅崇睿
 */
@Component
public class TokenProviderAdapter implements ITokenProvider {

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public String createToken(Long userId, String username, String role) {
        return jwtTokenProvider.createToken(userId, username, role);
    }
}
