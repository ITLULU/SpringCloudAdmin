package com.opensabre.admin.security.token;

import com.opensabre.admin.common.util.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * JWT认证过滤器 - 从请求头解析Token并设置SecurityContext
 * <p>
 * 认证流程：
 * 1. 从 Authorization 请求头提取 Bearer Token
 * 2. 校验 Token 有效性（签名、过期时间）
 * 3. 从 Token 中解析用户名，加载用户详情
 * 4. 校验用户状态（是否启用、是否锁定）
 * 5. 设置 SecurityContext 和 UserContextHolder（供业务层使用）
 * </p>
 * <p>
 * 注意：此过滤器不会拦截请求。如果 Token 无效或缺失，请求仍以匿名身份继续，
 * 后续的 Security 授权规则（@PreAuthorize / permitAll）决定是否放行。
 * </p>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 提取 Token（无 Token 则跳过，走匿名访问逻辑）
            String token = resolveToken(request);
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 校验 Token 有效性（签名校验 + 过期校验）
            if (!jwtTokenProvider.validateToken(token)) {
                log.debug("JWT Token无效或已过期, URI: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 从 Token 中解析用户名
            String username = jwtTokenProvider.getUsernameFromToken(token);
            if (!StringUtils.hasText(username)) {
                log.warn("JWT Token中用户名为空, URI: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // 4. 如果 SecurityContext 中已有认证信息，不重复加载
            SecurityContext context = SecurityContextHolder.getContext();
            if (context.getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 5. 加载用户详情（含角色、权限）
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(username);
            } catch (Exception e) {
                // 用户不存在、被禁用、被锁定等情况
                log.warn("加载用户信息失败 [{}]: {}", username, e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

            // 6. 校验用户状态（是否启用、账户是否过期/锁定）
            if (!isUserValid(userDetails)) {
                log.warn("用户状态异常 [{}]，拒绝认证。enabled={}, accountNonExpired={}, accountNonLocked={}, credentialsNonExpired={}",
                        username,
                        userDetails.isEnabled(),
                        userDetails.isAccountNonExpired(),
                        userDetails.isAccountNonLocked(),
                        userDetails.isCredentialsNonExpired());
                filterChain.doFilter(request, response);
                return;
            }

            // 7. 构建认证对象并设置到 SecurityContext
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            context.setAuthentication(authentication);

            // 8. 设置用户上下文（供业务层通过 UserContextHolder 获取当前用户名）
            UserContextHolder.getInstance().setContext(Map.of(UserContextHolder.KEY_USERNAME, username));

            log.debug("JWT认证成功: {}, URI: {}", username, request.getRequestURI());

        } finally {
            // 请求结束后清理用户上下文，防止线程池复用导致数据污染
            UserContextHolder.getInstance().clear();
        }

        // 继续执行后续过滤器和业务逻辑
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer Token
     *
     * @param request HTTP请求
     * @return Token字符串，不存在则返回null
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 校验用户状态是否有效
     * <p>
     * Spring Security 的 UserDetails 提供了四个状态方法：
     * - isEnabled(): 账户是否启用
     * - isAccountNonExpired(): 账户是否未过期
     * - isAccountNonLocked(): 账户是否未锁定
     * - isCredentialsNonExpired(): 凭证（密码）是否未过期
     * </p>
     *
     * @param userDetails 用户详情
     * @return true=用户有效，false=用户状态异常
     */
    private boolean isUserValid(UserDetails userDetails) {
        return userDetails != null && userDetails.isEnabled()
                && userDetails.isAccountNonExpired()
                && userDetails.isAccountNonLocked()
                && userDetails.isCredentialsNonExpired();
    }
}
