package cn.fcr.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;

/**
 * Spring Security 安全配置
 *
 * <p>配置 HTTP 安全策略、JWT 过滤器链和密码编码器。
 * 无状态 Session 策略，管理接口需 ADMIN 角色。</p>
 *
 * @author 傅崇睿
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** JWT 认证过滤器 */
    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置安全过滤链
     *
     * <p>禁用 CSRF、使用无状态Session、配置路径访问权限、
     * 注册 JWT 过滤器到 UsernamePasswordAuthenticationFilter 之前。</p>
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeRequests(auth -> auth
                // CORS 预检请求放行（浏览器预检不带 Authorization 头，收窄白名单后必须显式放行）
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 支付宝同步跳转页与静态资源
                .antMatchers(
                    "/orders",
                    "/orders/**",
                    "/",
                    "/index.html",
                    "/static/**",
                    "/uploads/**",
                    "/*.js",
                    "/*.css",
                    "/*.html",
                    "/error"
                ).permitAll()
                // 微信扫码登录与微信开放网关（登录前流程，必须公开）
                .antMatchers("/pay-api/v1/login/**", "/pay-api/v1/weixin/**").permitAll()
                // 支付宝异步回调（支付宝服务器调用，无 JWT，必须公开）
                .antMatchers("/pay-api/v1/alipay/alipay_notify_url").permitAll()
                // 商城登录/注册与商品浏览（公开接口；categories 此前漏配白名单导致匿名 403，一并修复）
                .antMatchers("/mall-api/v1/auth/login", "/mall-api/v1/auth/register").permitAll()
                .antMatchers("/mall-api/v1/products", "/mall-api/v1/products/**", "/mall-api/v1/categories").permitAll()
                .antMatchers("/mall-api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
