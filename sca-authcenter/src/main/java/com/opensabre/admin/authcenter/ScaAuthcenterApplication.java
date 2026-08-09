package com.opensabre.admin.authcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 认证授权中心启动类
 * <p>
 * 基于 Spring Authorization Server（Spring 官方 OAuth2 授权服务器，Boot 3 兼容），
 * 支持 授权码+PKCE（authorization_code）、refresh_token 两种授权模式。
 * </p>
 * <p>
 * 组件扫描说明：
 * - com.opensabre.admin.authcenter: 本模块自身代码
 * - com.opensabre.admin.security.service: 复用 sca-security 的 UserDetailsService 实现
 *   （从 sys_user/sys_role/sys_menu 表加载用户、角色、权限）
 * 注意：sca-security 的资源服务器自动配置（SecurityConfig）已通过
 * application.yml 的 spring.autoconfigure.exclude 排除，避免与本模块的
 * 授权服务器安全过滤链冲突。
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ScaAuthcenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScaAuthcenterApplication.class, args);
    }
}
