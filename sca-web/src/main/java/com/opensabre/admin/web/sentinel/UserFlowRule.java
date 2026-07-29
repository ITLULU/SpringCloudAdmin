package com.opensabre.admin.web.sentinel;

import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import com.alibaba.csp.sentinel.util.StringUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户维度限流规则（自定义 UserFlowSlot 使用）
 * <p>
 * 语义：单个登录用户在 windowSeconds 秒的窗口内，对 resource 最多允许 countPerUser 次请求。
 * <p>
 * 规则由 Nacos 动态推送（data-id: sca-web-sentinel-userflow, group: SENTINEL_GROUP），JSON 格式：
 * <pre>
 * [{"resource":"/api/hotel/order","countPerUser":5,"windowSeconds":60}]
 * </pre>
 * 继承 {@link AbstractRule} 以复用 resource 字段，并兼容 BlockException.getRule() 的日志输出。
 */
@Getter
@Setter
public class UserFlowRule extends AbstractRule {

    /**
     * 单个用户在一个窗口内允许的最大请求次数
     */
    private int countPerUser;

    /**
     * 窗口长度（秒）
     */
    private int windowSeconds;

    /**
     * 规则合法性校验（非法规则在加载时被丢弃）
     */
    public boolean isValid() {
        return StringUtil.isNotBlank(getResource()) && countPerUser > 0 && windowSeconds > 0;
    }

    @Override
    public String toString() {
        return "UserFlowRule{resource=" + getResource()
                + ", countPerUser=" + countPerUser
                + ", windowSeconds=" + windowSeconds + "}";
    }
}
