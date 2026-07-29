package com.opensabre.admin.web.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;

/**
 * 用户维度限流异常：单个用户在窗口内的请求次数超过 UserFlowRule 阈值时抛出
 * <p>
 * ruleLimitApp 位携带触发限流的用户名，便于 BlockExceptionHandler 记录审计日志。
 * 由 SentinelConfig 的 BlockExceptionHandler 统一转换为 429 + code 1012。
 */
public class UserFlowException extends BlockException {

    public UserFlowException(String username, UserFlowRule rule) {
        super(username, rule);
    }

    /**
     * 触发限流的用户名（即 ruleLimitApp 位）
     */
    public String getUsername() {
        return getRuleLimitApp();
    }
}
