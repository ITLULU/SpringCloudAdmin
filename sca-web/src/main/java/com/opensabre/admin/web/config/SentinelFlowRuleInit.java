//package com.opensabre.admin.web.config;
//
//import com.alibaba.csp.sentinel.slots.block.RuleConstant;
//import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
//import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Sentinel 默认流控规则初始化
// * <p>
// * 注册默认 QPS 限流规则，Nacos 配置加载后会覆盖此处的默认值。
// * <p>
// * 资源命名规范：对应 Controller 方法的 @SentinelResource value
// */
//@Slf4j
//@Configuration
//public class SentinelFlowRuleInit {
//
//    @PostConstruct
//    public void init() {
//        List<FlowRule> rules = new ArrayList<>();
//
//        // 创建订单（AT模式）：QPS 限流 10
//        rules.add(createFlowRule("createOrder", 10));
//
//        // 创建订单（TCC模式）：QPS 限流 10
//        rules.add(createFlowRule("createOrderByTcc", 10));
//
//        FlowRuleManager.loadRules(rules);
//        log.info("[Sentinel] 默认流控规则已加载: {} 条", rules.size());
//    }
//
//    /**
//     * 创建 QPS 限流规则
//     *
//     * @param resource  资源名称（对应 @SentinelResource value）
//     * @param threshold QPS 阈值
//     */
//    private FlowRule createFlowRule(String resource, double threshold) {
//        FlowRule rule = new FlowRule();
//        rule.setResource(resource);
//        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
//        rule.setCount(threshold);
//        return rule;
//    }
//}
