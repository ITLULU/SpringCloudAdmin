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
 *   renewal:
 *     mode: sliding-window
 *     refresh-token-expiration: 604800000
 *     sliding-window-threshold: 0.33
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
     * Token续期策略配置
     */
    private Renewal renewal = new Renewal();

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
         * AccessToken过期时间（毫秒），默认24小时
         */
        private long expiration = 86400000L;
    }

    /**
     * Token续期策略配置
     * <p>
     * 支持三种模式：
     * - none: 不续期，Token过期后必须重新登录
     * - refresh-token: 双Token机制，AccessToken短效 + RefreshToken长效
     * - sliding-window: 滑动窗口，每次请求自动检查并续期
     * </p>
     */
    @Data
    public static class Renewal {
        /**
         * 续期模式：none / refresh-token / sliding-window
         */
        private RenewalMode mode = RenewalMode.NONE;

        /**
         * RefreshToken过期时间（毫秒），默认7天
         * 仅在 mode=refresh-token 时生效
         */
        private long refreshTokenExpiration = 604800000L;

        /**
         * 滑动窗口续期阈值（0~1之间的比例）
         * 当Token剩余时间 < 总时间 * threshold 时触发续期
         * 例如：0.33 表示剩余不足1/3时自动续期
         * 仅在 mode=sliding-window 时生效
         */
        private double slidingWindowThreshold = 0.33;
    }

    /**
     * 续期模式枚举
     */
    public enum RenewalMode {
        /**
         * 不续期，过期必须重新登录
         */
        NONE,
        /**
         * 双Token机制（AccessToken + RefreshToken）
         */
        REFRESH_TOKEN,
        /**
         * 滑动窗口自动续期
         */
        SLIDING_WINDOW
    }
}