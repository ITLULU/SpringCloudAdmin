package com.opensabre.admin.authcenter.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * OAuth2 授权服务器核心配置
 * <p>
 * 对应 tulingmall-authcenter 中 TulingAuthorizationServerConfig + JwtTokenStoreConfig
 * 的职责，但采用 Spring 官方 Authorization Server 实现（Boot 3 兼容）：
 * - 客户端注册：InMemoryRegisteredClientRepository（企业生产建议 JDBC 持久化）
 * - Token 存储：JWT（RSA 非对称签名），公钥通过 /oauth2/jwks 端点暴露
 * - 授权模式：authorization_code + PKCE、refresh_token（OAuth 2.1 已移除密码模式）
 * </p>
 */
@Configuration
public class AuthorizationServerConfig {

    /**
     * 前端 SPA 客户端ID
     */
    public static final String CLIENT_ID = "sc-admin-front";

    /**
     * 前端授权码回调地址
     */
    public static final String REDIRECT_URI = "http://localhost:3000/oauth2/callback";

    /**
     * 未登录时重定向到的前端登录页（登录表单 POST 回本服务 /login）
     */
    public static final String FRONTEND_LOGIN_PAGE = "http://localhost:3000/login";

    /**
     * 授权服务器安全过滤链（优先级最高）
     * <p>
     * 自动装配 /oauth2/authorize、/oauth2/token、/oauth2/jwks、
     * /.well-known/openid-configuration 等协议端点。
     * </p>
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // 授权端点未认证时，重定向到前端登录页（浏览器 HTML 请求才重定向，API 请求返回 401）
        http.exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint(FRONTEND_LOGIN_PAGE),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

        return http.build();
    }

    /**
     * 客户端注册信息
     * <p>
     * SPA 属于公共客户端（Public Client），无 client_secret，
     * 必须启用 PKCE（requireProofKey=true）防止授权码拦截攻击。
     * </p>
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient spaClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(CLIENT_ID)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(REDIRECT_URI)
                .scope("admin")
                // 公共客户端必须启用 PKCE；授权无需用户确认页（第一方应用）
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        // AccessToken 有效期 2 小时（原 sca-security 为 24 小时，OAuth2 惯例更短）
                        .accessTokenTimeToLive(Duration.ofHours(2))
                        // RefreshToken 有效期 7 天
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(spaClient);
    }

    /**
     * JWT 内容增强器：将用户的角色/权限写入 AccessToken 的 claims
     * <p>
     * 对应 tulingmall 的 TulingTokenEnhancer。资源服务器（sca-web 等）
     * 通过 claims 中的 roles/permissions 还原权限，无需再查库。
     * - roles: ROLE_ 开头的权限（如 ROLE_admin）
     * - permissions: 权限标识（如 system:user:list）
     * </p>
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())
                    && context.getPrincipal().getPrincipal() instanceof UserDetails userDetails) {
                List<String> roles = new ArrayList<>();
                List<String> permissions = new ArrayList<>();
                for (GrantedAuthority authority : userDetails.getAuthorities()) {
                    String value = authority.getAuthority();
                    if (value.startsWith("ROLE_")) {
                        roles.add(value);
                    } else {
                        permissions.add(value);
                    }
                }
                context.getClaims().claim("roles", roles);
                context.getClaims().claim("permissions", permissions);
            }
        };
    }

    /**
     * JWT 解码器（授权服务器内部使用）
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * JWK 公钥源：启动时生成 RSA-2048 密钥对
     * <p>
     * 对应 tulingmall 的 jwt.jks 非对称密钥方案。
     * 注意：此处为演示实现，每次重启密钥会更换（已签发的 token 将验签失败）。
     * 企业生产环境应使用固定的 JKS 密钥库文件或 KMS，并通过环境变量/配置中心注入密钥口令，
     * 参考 tulingmall 的 JwtCAProperties + KeyStoreKeyFactory 方式。
     * </p>
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    /**
     * 授权服务器端点配置
     * <p>
     * issuer 必须与资源服务器配置的 issuer-uri 一致，
     * 资源服务器据此发现 /oauth2/jwks 公钥端点完成验签。
     * </p>
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:9999")
                .build();
    }
}
