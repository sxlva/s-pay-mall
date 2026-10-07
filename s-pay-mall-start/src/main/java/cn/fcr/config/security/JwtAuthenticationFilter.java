package cn.fcr.config.security;

import cn.fcr.infrastructure.auth.token.JwtTokenProvider;
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
 * 解析失败仅记录日志并清空安全上下文，请求以匿名身份继续：
 * 受保护端点由 SecurityConfig 注册的 AuthenticationEntryPoint 统一返回 401（0003 未登录），
 * 公开端点（permitAll）不受过期 token 影响（S-02：不再静默放行穿透到 Controller 仅靠二次解析兜底）。</p>
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
     * 构建 Authentication 对象并写入 SecurityContextHolder；
     * 解析失败时清空安全上下文，交由 Security 层按路径规则决定是否放行。</p>
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
                // S-02：解析失败清空上下文（防止残留认证），不静默"假装成功"；
                // 未认证请求继续走过滤器链，受保护端点由 EntryPoint 统一 401，公开端点正常放行
                log.warn("JWT鉴权失败 - Authorization header: {}, 错误: {}", header, e.getMessage());
                SecurityContextHolder.clearContext();
            }
        } else {
            log.debug("JWT鉴权跳过 - Authorization header: {}", header != null ? header : "请求头为空");
        }
        filterChain.doFilter(request, response);
    }
}
