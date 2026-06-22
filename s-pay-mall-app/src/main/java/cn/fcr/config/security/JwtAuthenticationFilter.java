package cn.fcr.config.security;

import cn.fcr.infrastructure.config.auth.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 *
 * <p>从请求头 Authorization 中解析 JWT token，验证后写入 Spring Security 安全上下文。
 * 解析失败不影响请求继续执行。</p>
 *
 * @author 傅崇睿
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT Token 提供者 */
    @Resource
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 对每个请求执行 JWT 认证
     *
     * <p>从 Authorization 头中提取 Bearer token，解析出用户名和角色，
     * 构建 Authentication 对象并写入 SecurityContextHolder。</p>
     *
     * @param request     HTTP请求
     * @param response    HTTP响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException      IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                log.debug("JWT Token解析前 - Authorization header: {}", header);
                Claims claims = jwtTokenProvider.parse(token);
                String username = (String) claims.get("username");
                String role = (String) claims.get("role");
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.warn("JWT鉴权失败 - Authorization header: {}, 错误: {}", header, e.getMessage());
            }
        } else {
            log.debug("JWT鉴权跳过 - Authorization header: {}", header != null ? header : "请求头为空");
        }
        filterChain.doFilter(request, response);
    }
}
