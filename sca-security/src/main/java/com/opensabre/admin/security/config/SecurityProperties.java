package com.opensabre.admin.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全配置属性 - 可通过application.yml配置放行路径和JWT参数
 * <p>
 * 示例配置:
 * <pre>
 * security:
 *   jwt:
 *     secret: your-256-bit-secret-key-here-must-be-long-enough
 *     expiration: 86400000
 *   permit-urls:
 *     - /api/auth/**
 *     - /actuator/**
 * </pre>
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * JWT配置
     */
    private Jwt jwt = new Jwt();

    /**
     * 放行URL路径列表（不需要认证即可访问的接口）
     */
    private List<String> permitUrls = new ArrayList<>();

    @Data
    public static class Jwt {
        /**
         * JWT签名密钥（至少32字符）
         */
        private String secret = "SpringCloudAdmin-Secret-Key-2024-Must-Be-At-Least-32-Chars";

        /**
         * Token过期时间（毫秒），默认24小时
         */
        private long expiration = 86400000L;
    }
}
