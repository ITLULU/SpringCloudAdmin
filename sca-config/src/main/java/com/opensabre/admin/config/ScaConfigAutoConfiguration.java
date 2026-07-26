package com.opensabre.admin.config;

import com.opensabre.admin.common.entity.RestResponseBodyAdvice;
import com.opensabre.admin.common.filter.UserContextFilter;
import com.opensabre.admin.config.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * 公共模块自动配置 - 注入响应体包装、用户上下文过滤器
 */
@AutoConfiguration
@Import({RestResponseBodyAdvice.class,GlobalExceptionHandler.class})
public class ScaConfigAutoConfiguration {

    /**
     * 注册用户上下文过滤器 - 从 Feign Header 读取 X-Username
     */
    @Bean
    public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration() {
        FilterRegistrationBean<UserContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UserContextFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("userContextFilter");
        return registration;
    }

    // TccAtExcludeFilter 已移除：真正的 Seata TCC 需要 XID 保留在 RootContext
    // 以便 TCC 代理注册分支到 TC。TCC 与 AT 共存时 AT undo_log 是无害的额外安全层。
}
