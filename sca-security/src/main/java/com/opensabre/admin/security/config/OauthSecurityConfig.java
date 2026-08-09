//package com.opensabre.admin.security.config;
//
//import com.opensabre.admin.security.handler.AccessDeniedHandlerImpl;
//import com.opensabre.admin.security.handler.AuthenticationEntryPointHandler;
//import com.opensabre.admin.security.token.JwtAuthorityConverter;
//import org.springframework.boot.autoconfigure.AutoConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
///**
// * Spring Security安全配置（资源服务器模式，适配 Spring Security 6.x / Spring Boot 3.x）
// * <p>
// * Token 验签方式：
// * - 由 sca-authcenter（Spring Authorization Server）签发 JWT（RSA 非对称签名）
// * - 资源服务通过 issuer-uri 发现授权服务器的 /oauth2/jwks 公钥端点完成本地验签，
// *   无需共享密钥（企业标准做法）
// * - 配置项：spring.security.oauth2.resourceserver.jwt.issuer-uri
// * </p>
// * <p>
// * 功能：
// * - 无状态JWT认证（不依赖Session）
// * - 可配置放行路径（通过 application.yml 的 security.permit-urls 配置）
// * - JWT claims 中的 roles/permissions 自动还原为 GrantedAuthority
// * </p>
// */
//
//@AutoConfiguration
//@EnableWebSecurity
//@EnableMethodSecurity
//@ComponentScan(basePackages = "com.opensabre.admin.security")
//public class OauthSecurityConfig {
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public JwtAuthorityConverter jwtAuthorityConverter() {
//        return new JwtAuthorityConverter();
//    }
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http,
//                                                   OauthSecurityProperties properties,
//                                                   JwtAuthorityConverter jwtAuthorityConverter,
//                                                   AuthenticationEntryPointHandler authenticationEntryPointHandler,
//                                                   AccessDeniedHandlerImpl accessDeniedHandler) throws Exception {
//        http
//                // 禁用CSRF（无状态JWT不需要）
//                .csrf(csrf -> csrf.disable())
//                // 无状态Session
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                // 配置请求授权规则
//                .authorizeHttpRequests(authorize -> {
//                    // 配置的放行路径（从application.yml读取）
//                    if (!properties.getPermitUrls().isEmpty()) {
//                        authorize.requestMatchers(properties.getPermitUrls().toArray(new String[0])).permitAll();
//                    }
//                    // 所有其他请求需要认证
//                    authorize.anyRequest().authenticated();
//                })
//                // OAuth2 资源服务器：JWT 验签（issuer-uri/jwks-uri 由 Spring Boot 自动配置）
//                .oauth2ResourceServer(oauth2 -> oauth2
//                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthorityConverter))
//                        .authenticationEntryPoint(authenticationEntryPointHandler)
//                        .accessDeniedHandler(accessDeniedHandler))
//                // 异常处理
//                .exceptionHandling(ex -> ex
//                        .authenticationEntryPoint(authenticationEntryPointHandler)
//                        .accessDeniedHandler(accessDeniedHandler));
//
//        return http.build();
//    }
//}