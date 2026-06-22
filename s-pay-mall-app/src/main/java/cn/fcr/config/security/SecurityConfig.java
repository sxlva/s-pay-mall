package cn.fcr.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                .antMatchers(
                    "/orders",
                    "/orders/**",
                    "/",
                    "/index.html",
                    "/static/**",
                    "/*.js",
                    "/*.css",
                    "/*.html",
                    "/pay-api/v1/login/**",
                    "/pay-api/v1/weixin/**",
                    "/pay-api/v1/alipay/**",
                    "/mall-api/v1/auth/**",
                    "/mall-api/v1/mall/user/**",
                    "/mall-api/v1/products/**",
                    "/mall-api/v1/orders/**",
                    "/mall-api/v1/cart/**",
                    "/mall-api/v1/profile/**",
                    "/error"
                ).permitAll()
                .antMatchers("/mall-api/v1/admin/**").hasRole("ADMIN")
                .antMatchers("/pay-api/v1/admin/**").hasRole("ADMIN")
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
