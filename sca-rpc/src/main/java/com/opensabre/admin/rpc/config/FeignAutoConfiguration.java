package com.opensabre.admin.rpc.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Feign自动配置 - 启用Feign客户端扫描
 */
@AutoConfiguration
@EnableFeignClients(basePackages = "com.opensabre.admin.rpc.client")
public class FeignAutoConfiguration {
}
