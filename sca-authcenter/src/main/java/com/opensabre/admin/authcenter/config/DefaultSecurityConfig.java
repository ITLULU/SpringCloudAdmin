package com.opensabre.admin.authcenter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * 默认安全配置（表单登录链）
 * <p>
 * 对应 tulingmall 的 WebSecurityConfig 职责。
 * 登录流程：
 * 1. 前端发起 /oauth2/authorize 授权请求，未登录时被重定向到前端登录页
 *    （http://localhost:3000/login）
 * 2. 前端登录表单以 application/x-www-form-urlencoded 方式
 *    POST username/password 到本服务 /login（经 vite 代理保持同源）
 * 3. 认证成功后，SavedRequestAwareAuthenticationSuccessHandler 自动恢复
 *    之前保存的授权请求，继续完成授权码流程
 * </p>
 */
@Configuration
@EnableWebSecurity
public class DefaultSecurityConfig {

    /**
     * 默认安全过滤链：所有请求需认证，登录页指向前端页面
     * <p>
     * CSRF 说明：此处禁用 CSRF 是因为 SPA 登录页与授权服务器跨端口部署，
     * 表单提交无法携带 CSRF Token。企业同域部署时应启用 CSRF 防护
     * （CookieCsrfTokenRepository + 前端携带 X-XSRF-TOKEN）。
     * </p>
     */
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated())
                // 登录页为前端 SPA 页面，登录处理地址保持默认 /login
                .formLogin(form -> form
                        .loginPage(AuthorizationServerConfig.FRONTEND_LOGIN_PAGE)
                        // 登录成功后：有保存的授权请求则恢复它（继续授权码流程）；
                        // 否则重定向回前端登录页并带 authorized=1 标记，
                        // 前端据此自动发起 /oauth2/authorize 授权请求
                        .successHandler(oauth2AwareSuccessHandler()));
        return http.build();
    }

    /**
     * 登录成功处理器：优先恢复保存的授权请求，无则回前端继续授权流程
     */
    private SavedRequestAwareAuthenticationSuccessHandler oauth2AwareSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler handler =
                new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl(AuthorizationServerConfig.FRONTEND_LOGIN_PAGE + "?authorized=1");
        return handler;
    }

    /**
     * 认证管理器：基于数据库用户（UserDetailsService 由 sca-security 提供）
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * 密码编码器（sys_user 表密码为 BCrypt 加密）
     * <p>
     * sca-security 的 SecurityConfig 被排除后，需由本模块自行提供该 Bean。
     * </p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
