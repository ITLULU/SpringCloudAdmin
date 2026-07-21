package com.opensabre.admin.common;

import com.opensabre.admin.common.entity.GlobalExceptionHandlerAdvice;
import com.opensabre.admin.common.entity.RestResponseBodyAdvice;
import com.opensabre.admin.common.filter.UserContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * 公共模块自动配置 - 注入全局异常处理、响应体包装、用户上下文过滤器
 */
@AutoConfiguration
@Import({GlobalExceptionHandlerAdvice.class, RestResponseBodyAdvice.class})
public class ScaCommonAutoConfiguration {

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
}
