package com.opensabre.admin.common;

import com.opensabre.admin.common.entity.GlobalExceptionHandlerAdvice;
import com.opensabre.admin.common.entity.RestResponseBodyAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 公共模块自动配置 - 注入全局异常处理和响应体包装
 */
@AutoConfiguration
@Import({GlobalExceptionHandlerAdvice.class, RestResponseBodyAdvice.class})
public class ScaCommonAutoConfiguration {
}
