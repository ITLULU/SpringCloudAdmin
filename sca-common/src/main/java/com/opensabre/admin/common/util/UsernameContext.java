package com.opensabre.admin.common.util;

/**
 * 用户上下文 ThreadLocal - 跨服务传递当前登录用户名
 * <p>
 * 使用场景：
 * - sca-web: JWT认证后通过 {@link #setUsername} 设置用户名
 * - sca-order/sca-stock: 通过 UserContextFilter 从 Feign Header 读取并设置
 * - PoMetaObjectHandler: 通过 {@link #getUsername} 获取审计字段填充值
 * </p>
 */
public final class UsernameContext {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private UsernameContext() {
    }

    /**
     * 设置当前线程的用户名
     */
    public static void setUsername(String username) {
        CONTEXT.set(username);
    }

    /**
     * 获取当前线程的用户名
     *
     * @return 用户名，未设置时返回 null
     */
    public static String getUsername() {
        return CONTEXT.get();
    }

    /**
     * 清除当前线程的用户名（防止内存泄漏）
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
