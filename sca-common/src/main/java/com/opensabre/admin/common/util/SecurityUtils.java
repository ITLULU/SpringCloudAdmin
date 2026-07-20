package com.opensabre.admin.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 安全工具类 - 从 Spring Security SecurityContext 获取当前登录用户信息
 * <p>
 * 在标准 Spring MVC（同步 Servlet）中，Filter 和 Controller 保证在同一线程，
 * SecurityContextHolder 默认使用 MODE_THREADLOCAL 存储，因此可以安全地在 Controller 中获取当前用户。
 * </p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户名
     *
     * @return 用户名，未登录时返回 null
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String str) {
            return str;
        }
        return null;
    }
}
