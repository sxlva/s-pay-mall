package cn.fcr.infrastructure.auth.gateway;

import cn.fcr.domain.auth.gateway.ITokenProvider;
import cn.fcr.infrastructure.config.auth.JwtTokenProvider;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @description Token提供者适配器
 *
 * 【职责说明】
 * - 适配JwtTokenProvider，实现领域层定义的ITokenProvider接口
 * - 负责创建JWT令牌
 *
 * 【核心功能】
 * 1. createToken(): 创建JWT令牌
 *
 * 【依赖说明】
 * - JwtTokenProvider: JWT令牌提供者
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
