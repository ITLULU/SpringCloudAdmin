package com.opensabre.admin.security.config;

import com.opensabre.admin.security.handler.AuthenticationEntryPointHandler;
import com.opensabre.admin.security.handler.AccessDeniedHandlerImpl;
import com.opensabre.admin.security.token.JwtAuthenticationFilter;
import com.opensabre.admin.security.token.JwtTokenProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security安全配置（适配 Spring Security 6.x / Spring Boot 3.x）
 * <p>
 * 功能：
 * - 无状态JWT认证（不依赖Session）
 * - 可配置放行路径（通过 application.yml 的 security.permit-urls 配置）
 * - JWT Token 自动解析与认证
 * </p>
 */
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@ComponentScan(basePackages = "com.opensabre.admin.security")
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider(SecurityProperties properties) {
        return new JwtTokenProvider(
                properties.getJwt().getSecret(),
                properties.getJwt().getExpiration()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    SecurityProperties properties,
                                                    JwtAuthenticationFilter jwtAuthenticationFilter,
                                                    AuthenticationEntryPointHandler authenticationEntryPointHandler,
                                                    AccessDeniedHandlerImpl accessDeniedHandler) throws Exception {
        http
            // 禁用CSRF（无状态JWT不需要）
            .csrf(csrf -> csrf.disable())
            // 无状态Session
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置请求授权规则
            .authorizeHttpRequests(authorize -> {
                // 配置的放行路径（从application.yml读取）
                if (!properties.getPermitUrls().isEmpty()) {
                    authorize.requestMatchers(properties.getPermitUrls().toArray(new String[0])).permitAll();
                }
                // 所有其他请求需要认证
                authorize.anyRequest().authenticated();
            })
            // 异常处理
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPointHandler)
                .accessDeniedHandler(accessDeniedHandler));

        // 在UsernamePasswordAuthenticationFilter之前添加JWT过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
