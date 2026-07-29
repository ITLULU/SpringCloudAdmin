package com.opensabre.admin.web.config;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensabre.admin.web.sentinel.UserFlowRule;
import com.opensabre.admin.web.sentinel.UserFlowRuleManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 用户维度限流规则的 Nacos 动态数据源装配
 * <p>
 * 自定义规则类型无法通过 spring.cloud.sentinel.datasource 声明（starter 的 rule-type 枚举
 * 仅支持 flow/degrade/authority 等内置类型），因此仿照内置规则的接入方式，
 * 启动时手工创建 NacosDataSource 并注册到 UserFlowRuleManager：
 * 启动即拉取全量规则 + Nacos 长轮询推送热更新，行为与 flow/degrade/authority 数据源一致。
 * <p>
 * Nacos 配置：data-id: ${spring.application.name}-sentinel-userflow，group: SENTINEL_GROUP，JSON 格式。
 */
@Slf4j
@Configuration
public class SentinelUserFlowConfig {

    private static final String RULE_GROUP = "SENTINEL_GROUP";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Value("${spring.cloud.nacos.config.server-addr}")
    private String serverAddr;

    @Value("${nacos.username:}")
    private String nacosUsername;

    @Value("${nacos.password:}")
    private String nacosPassword;

    @Value("${spring.application.name}")
    private String applicationName;

    private NacosDataSource<List<UserFlowRule>> dataSource;

    @PostConstruct
    public void init() {
        String dataId = applicationName + "-sentinel-userflow";
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
        if (StringUtils.hasText(nacosUsername)) {
            properties.put(PropertyKeyConst.USERNAME, nacosUsername);
            properties.put(PropertyKeyConst.PASSWORD, nacosPassword);
        }

        Converter<String, List<UserFlowRule>> parser = source -> {
            if (!StringUtils.hasText(source)) {
                return Collections.emptyList();
            }
            try {
                return OBJECT_MAPPER.readValue(source, new TypeReference<List<UserFlowRule>>() {
                });
            } catch (Exception e) {
                log.error("[Sentinel][UserFlow] 规则解析失败, dataId={}, content={}", dataId, source, e);
                throw new IllegalArgumentException("用户限流规则解析失败", e);
            }
        };

        dataSource = new NacosDataSource<>(properties, RULE_GROUP, dataId, parser);
        UserFlowRuleManager.register2Property(dataSource.getProperty());
        log.info("[Sentinel][UserFlow] Nacos 动态数据源已注册: dataId={}, group={}", dataId, RULE_GROUP);
    }

    @PreDestroy
    public void destroy() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (Exception e) {
                log.warn("[Sentinel][UserFlow] 数据源关闭异常", e);
            }
        }
    }
}
