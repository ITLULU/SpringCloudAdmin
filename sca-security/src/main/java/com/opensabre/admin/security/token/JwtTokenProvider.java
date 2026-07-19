package com.opensabre.admin.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token 工具类 - 负责Token的生成、解析和校验
 * <p>
 * 支持两种Token类型：
 * - AccessToken: 短期有效，用于API访问认证
 * - RefreshToken: 长期有效，仅用于刷新AccessToken（不能直接访问API）
 * </p>
 */
@Slf4j
public class JwtTokenProvider {

    /**
     * Token类型Claim Key - 用于区分AccessToken和RefreshToken
     */
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long expiration;
    private final long refreshExpiration;

    public JwtTokenProvider(String secret, long expiration, long refreshExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * 创建AccessToken（用于API访问认证）
     *
     * @param username 用户名
     * @return AccessToken字符串
     */
    public String createAccessToken(String username) {
        return createToken(username, TOKEN_TYPE_ACCESS, expiration);
    }

    /**
     * 创建RefreshToken（仅用于刷新AccessToken）
     *
     * @param username 用户名
     * @return RefreshToken字符串
     */
    public String createRefreshToken(String username) {
        return createToken(username, TOKEN_TYPE_REFRESH, refreshExpiration);
    }

    /**
     * 创建JWT Token（内部方法）
     *
     * @param username     用户名
     * @param tokenType    Token类型（access/refresh）
     * @param expirationMs 过期时间（毫秒）
     * @return Token字符串
     */
    private String createToken(String username, String tokenType, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 从Token中获取过期时间
     *
     * @param token JWT Token
     * @return 过期时间戳（毫秒）
     */
    public long getExpirationFromToken(String token) {
        return getClaims(token).getExpiration().getTime();
    }

    /**
     * 获取Token剩余有效时间（毫秒）
     *
     * @param token JWT Token
     * @return 剩余时间（毫秒），已过期返回负数
     */
    public long getRemainingTime(String token) {
        try {
            long expirationTime = getExpirationFromToken(token);
            return expirationTime - System.currentTimeMillis();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 判断Token是否需要续期（滑动窗口用）
     *
     * @param token     JWT Token
     * @param threshold 阈值比例（0~1），剩余时间占比低于此值时返回true
     * @return true=需要续期，false=不需要
     */
    public boolean shouldRenew(String token, double threshold) {
        try {
            long remaining = getRemainingTime(token);
            long total = expiration;
            // 剩余时间 / 总时间 < 阈值 → 需要续期
            return remaining > 0 && (double) remaining / total < threshold;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验Token是否有效（通用校验）
     *
     * @param token JWT Token
     * @return true=有效，false=无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT Token已过期");
        } catch (Exception e) {
            log.debug("JWT Token无效: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 校验是否为有效的AccessToken
     *
     * @param token JWT Token
     * @return true=是有效的AccessToken
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = getClaims(token);
            String type = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return TOKEN_TYPE_ACCESS.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验是否为有效的RefreshToken
     *
     * @param token JWT Token
     * @return true=是有效的RefreshToken
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaims(token);
            String type = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return TOKEN_TYPE_REFRESH.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析Token的Claims（内部方法）
     *
     * @param token JWT Token
     * @return Claims对象
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
