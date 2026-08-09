//package com.opensabre.admin.security.config;
//
//import lombok.Data;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * 安全配置属性 - 可通过application.yml配置放行路径
// * <p>
// * Token 签发与续期已由 sca-authcenter（Spring Authorization Server）统一负责，
// * 资源服务器侧通过标准配置项验签：
// * <pre>
// * spring:
// *   security:
// *     oauth2:
// *       resourceserver:
// *         jwt:
// *           issuer-uri: http://localhost:9999
// * </pre>
// * 本属性仅保留放行路径配置：
// * <pre>
// * security:
// *   permit-urls:
// *     - /api/auth/register
// *     - /actuator/**
// * </pre>
// * </p>
// */
//@Data
//@Component
//@ConfigurationProperties(prefix = "security")
//public class OauthSecurityProperties {
//
//    /**
//     * 放行 URL路径列表（不需要认证即可访问的接口）
//     */
//    private List<String> permitUrls = new ArrayList<>();
//}
