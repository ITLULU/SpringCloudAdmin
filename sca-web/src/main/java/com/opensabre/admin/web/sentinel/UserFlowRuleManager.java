package com.opensabre.admin.web.sentinel;

import com.alibaba.csp.sentinel.property.DynamicSentinelProperty;
import com.alibaba.csp.sentinel.property.PropertyListener;
import com.alibaba.csp.sentinel.property.SentinelProperty;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户维度限流规则管理器（仿 FlowRuleManager 的 SentinelProperty 监听模式）
 * <p>
 * 规则来源：SentinelUserFlowConfig 启动时注册的 NacosDataSource
 * （data-id: sca-web-sentinel-userflow, group: SENTINEL_GROUP），
 * Nacos 长轮询推送后原子替换规则表，热更新无需重启。
 */
@Slf4j
public final class UserFlowRuleManager {

    /**
     * resource -> 规则，volatile 引用整体替换保证读侧无锁
     */
    private static volatile Map<String, UserFlowRule> ruleMap = new ConcurrentHashMap<>();

    private static final PropertyListener<List<UserFlowRule>> LISTENER = new RulePropertyListener();

    private static SentinelProperty<List<UserFlowRule>> currentProperty = new DynamicSentinelProperty<>();

    static {
        currentProperty.addListener(LISTENER);
    }

    private UserFlowRuleManager() {
    }

    /**
     * 注册动态规则源（如 NacosDataSource.getProperty()），替换默认的静态 Property
     */
    public static synchronized void register2Property(SentinelProperty<List<UserFlowRule>> property) {
        if (property == null) {
            return;
        }
        currentProperty.removeListener(LISTENER);
        property.addListener(LISTENER);
        currentProperty = property;
        log.info("[Sentinel][UserFlow] 用户限流规则已切换为动态数据源");
    }

    /**
     * 直接加载规则（静态配置或测试用）
     */
    public static void loadRules(List<UserFlowRule> rules) {
        currentProperty.updateValue(rules);
    }

    public static UserFlowRule getRule(String resource) {
        return ruleMap.get(resource);
    }

    public static boolean hasRules() {
        return !ruleMap.isEmpty();
    }

    public static List<UserFlowRule> getRules() {
        return new ArrayList<>(ruleMap.values());
    }

    private static Map<String, UserFlowRule> buildRuleMap(List<UserFlowRule> rules) {
        Map<String, UserFlowRule> newMap = new ConcurrentHashMap<>();
        if (rules == null) {
            return newMap;
        }
        for (UserFlowRule rule : rules) {
            if (rule == null || !rule.isValid()) {
                log.warn("[Sentinel][UserFlow] 忽略非法规则: {}", rule);
                continue;
            }
            newMap.put(rule.getResource(), rule);
        }
        return newMap;
    }

    private static class RulePropertyListener implements PropertyListener<List<UserFlowRule>> {

        @Override
        public void configUpdate(List<UserFlowRule> rules) {
            ruleMap = buildRuleMap(rules);
            log.info("[Sentinel][UserFlow] 用户限流规则已更新: {} 条 -> {}", ruleMap.size(), ruleMap.values());
        }

        @Override
        public void configLoad(List<UserFlowRule> rules) {
            ruleMap = buildRuleMap(rules);
            log.info("[Sentinel][UserFlow] 用户限流规则已加载: {} 条 -> {}", ruleMap.size(), ruleMap.values());
        }
    }
}
