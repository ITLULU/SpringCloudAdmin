package com.opensabre.admin.common.filter;

import com.opensabre.admin.common.util.UsernameContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 用户上下文过滤器 - 从请求头读取 X-Username 并设置到 UsernameContext
 * <p>
 * 用于下游微服务（如 sca-order、sca-stock）接收 Feign 调用时获取调用方传递的用户名。
 * 请求结束后自动清理 ThreadLocal 防止内存泄漏。
 * </p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserContextFilter implements Filter {

    private static final String HEADER_USERNAME = "X-Username";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String username = httpRequest.getHeader(HEADER_USERNAME);
            if (StringUtils.hasText(username)) {
                UsernameContext.setUsername(username);
            }
            chain.doFilter(request, response);
        } finally {
            UsernameContext.clear();
        }
    }
}
