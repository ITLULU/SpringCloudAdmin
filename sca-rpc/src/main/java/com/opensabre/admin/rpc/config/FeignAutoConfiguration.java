package com.opensabre.admin.rpc.config;

import feign.Logger;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * Feign自动配置 - 启用Feign客户端扫描 + 日志配置
 */
@AutoConfiguration
@EnableFeignClients(basePackages = "com.opensabre.admin.rpc.client")
public class FeignAutoConfiguration {

    /**
     * Feign日志级别
     * NONE: 不记录任何日志（默认）
     * BASIC: 记录请求方法、URL、响应状态码、执行时间
     * HEADERS: BASIC + 请求/响应头
     * FULL: 记录请求/响应的头、体、元数据
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
