package com.opensabre.admin.web.sentinel;

import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slotchain.AbstractLinkedProcessorSlot;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.spi.Spi;
import com.opensabre.admin.common.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义 ProcessorSlot：用户维度限流（防刷）
 * <p>
 * 通过 Sentinel SPI 机制注册（META-INF/services/com.alibaba.csp.sentinel.slotchain.ProcessorSlot），
 * order = -2500，编排在 AuthoritySlot(-6000)/SystemSlot(-5000)/ParamFlowSlot(-3000) 之后、
 * FlowSlot(-2000) 之前：恶意用户先被单独拦截，不占用接口总 QPS 配额。
 * <p>
 * 检查逻辑（fail-open，只做已登录用户防刷，不承担认证职责）：
 * 1. 仅处理入站（EntryType.IN）且配置了 UserFlowRule 的资源；
 * 2. 用户名取自 SecurityContextHolder（SentinelWebInterceptor 与 JwtAuthenticationFilter 同线程），
 *    未登录/匿名请求直接放行；
 * 3. 单用户窗口内计数超过阈值抛 {@link UserFlowException}，
 *    由 SentinelConfig 的 BlockExceptionHandler 转换为 429 + code 1012。
 */
@Slf4j
@Spi(order = -2500)
public class UserFlowSlot extends AbstractLinkedProcessorSlot<DefaultNode> {

    /**
     * Spring Security 匿名认证的 principal 标识
     */
    private static final String ANONYMOUS_USER = "anonymousUser";

    @Override
    public void entry(Context context, ResourceWrapper resourceWrapper, DefaultNode node,
                      int count, boolean prioritized, Object... args) throws Throwable {
        checkUserFlow(resourceWrapper);
        fireEntry(context, resourceWrapper, node, count, prioritized, args);
    }

    @Override
    public void exit(Context context, ResourceWrapper resourceWrapper, int count, Object... args) {
        fireExit(context, resourceWrapper, count, args);
    }

    private void checkUserFlow(ResourceWrapper resourceWrapper) throws UserFlowException {
        // 快速路径：无任何规则或非入站资源（Feign 出站为 OUT）直接放行
        if (!UserFlowRuleManager.hasRules() || resourceWrapper.getEntryType() != EntryType.IN) {
            return;
        }
        UserFlowRule rule = UserFlowRuleManager.getRule(resourceWrapper.getName());
        if (rule == null) {
            return;
        }
        // 未登录/匿名请求放行（登录校验由 Spring Security 负责）
        String username = SecurityUtils.getCurrentUsername();
        if (username == null || username.isBlank() || ANONYMOUS_USER.equals(username)) {
            return;
        }
        if (!UserFlowChecker.tryPass(resourceWrapper.getName(), username, rule)) {
            log.warn("[Sentinel][UserFlow] 用户维度限流触发: resource={}, user={}, rule={}",
                    resourceWrapper.getName(), username, rule);
            throw new UserFlowException(username, rule);
        }
    }
}
