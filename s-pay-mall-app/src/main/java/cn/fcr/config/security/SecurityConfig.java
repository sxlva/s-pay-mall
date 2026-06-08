package cn.fcr.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeRequests(auth -> auth
                .antMatchers("/orders", "/orders/**").permitAll()
                .antMatchers(
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
                    "/mall-api/v1/admin/**",
                    "/mall-api/v1/profile/**",
                    "/error"
                ).permitAll()
                .antMatchers("/pay-api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}