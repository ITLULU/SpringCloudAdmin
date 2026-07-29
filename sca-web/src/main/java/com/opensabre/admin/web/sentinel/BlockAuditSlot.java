package com.opensabre.admin.web.sentinel;

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slotchain.AbstractLinkedProcessorSlot;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.alibaba.csp.sentinel.spi.Spi;
import com.opensabre.admin.common.message.SentinelBlockMessage;
import com.opensabre.admin.common.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel 拦截事件审计 Slot（观测型，不改变流控行为）
 * <p>
 * 通过 SPI 注册到 Slot 链，order = -6500，位于 StatisticSlot(-7000) 之后、
 * AuthoritySlot(-6000) 之前。审计 Slot 对 fireEntry 做 try-catch，
 * 可捕获下游所有拦截型 Slot 抛出的 BlockException（授权/系统/热点参数/用户限流/流控/熔断），
 * 记录"谁、什么时候、哪个接口、命中哪条规则"后将异常原样抛出，不影响原有拦截语义。
 * <p>
 * 事件通过 {@link BlockAuditEventPublisher} 有界队列异步推送 Kafka，
 * 队列满或 Kafka 不可用时直接丢弃（fail-open），绝不阻塞业务请求线程。
 */
@Slf4j
@Spi(order = -6500)
public class BlockAuditSlot extends AbstractLinkedProcessorSlot<DefaultNode> {

    private static final String ANONYMOUS_USER = "anonymousUser";

    @Override
    public void entry(Context context, ResourceWrapper resourceWrapper, DefaultNode node,
                      int count, boolean prioritized, Object... args) throws Throwable {
        try {
            fireEntry(context, resourceWrapper, node, count, prioritized, args);
        } catch (BlockException e) {
            recordBlockEvent(context, resourceWrapper, e);
            throw e;
        }
    }

    @Override
    public void exit(Context context, ResourceWrapper resourceWrapper, int count, Object... args) {
        fireExit(context, resourceWrapper, count, args);
    }

    /**
     * 构建审计事件并异步投递，任何异常只记日志，不影响主流程
     */
    private void recordBlockEvent(Context context, ResourceWrapper resourceWrapper, BlockException e) {
        try {
            SentinelBlockMessage event = new SentinelBlockMessage();
            event.setResource(resourceWrapper.getName());
            event.setBlockType(resolveBlockType(e));
            event.setUsername(resolveUsername());
            event.setOrigin(context == null ? null : context.getOrigin());
            event.setRuleInfo(e.getRule() == null ? null : e.getRule().toString());
            event.setBlockTime(System.currentTimeMillis());
            BlockAuditEventPublisher.publishBlockEvent(event);
        } catch (Throwable t) {
            log.warn("[Sentinel][BlockAudit] 审计事件记录失败（不影响业务）: resource={}, error={}",
                    resourceWrapper.getName(), t.getMessage());
        }
    }

    /**
     * 按异常类型归类拦截类型，注意 UserFlowException 需在通用类型之前判断
     */
    private String resolveBlockType(BlockException e) {
        if (e instanceof UserFlowException) {
            return "USER_FLOW";
        } else if (e instanceof AuthorityException) {
            return "AUTHORITY";
        } else if (e instanceof FlowException) {
            return "FLOW";
        } else if (e instanceof DegradeException) {
            return "DEGRADE";
        } else if (e instanceof ParamFlowException) {
            return "PARAM_FLOW";
        } else if (e instanceof SystemBlockException) {
            return "SYSTEM";
        }
        return "OTHER";
    }

    /**
     * 获取当前登录用户名，未登录/匿名用户返回 null
     */
    private String resolveUsername() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null || username.isBlank() || ANONYMOUS_USER.equals(username)) {
            return null;
        }
        return username;
    }
}
