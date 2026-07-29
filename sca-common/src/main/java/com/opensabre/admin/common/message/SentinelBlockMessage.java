package com.opensabre.admin.common.message;

import lombok.Data;

import java.io.Serializable;

/**
 * Sentinel 拦截审计消息体
 * <p>
 * 由 sca-web 的 BlockAuditSlot 在捕获 BlockException 时异步发送到 Kafka，
 * 由 sca-datasync 消费后写入 Elasticsearch（sentinel_block_index），用于审计与分析。
 */
@Data
public class SentinelBlockMessage implements Serializable {

    /** 触发拦截的服务名，如 sca-web */
    private String app;

    /** 被拦截的资源名（URL），如 /api/hotel/order */
    private String resource;

    /** 拦截类型：FLOW/DEGRADE/AUTHORITY/USER_FLOW/PARAM_FLOW/SYSTEM/OTHER */
    private String blockType;

    /** 触发拦截的登录用户名，未登录为 null */
    private String username;

    /** 调用来源（RequestOriginParser 解析的 originSource 请求头），如 sc-web/unknown */
    private String origin;

    /** 命中的规则描述（rule.toString()） */
    private String ruleInfo;

    /** 拦截发生时间戳（毫秒） */
    private Long blockTime;
}
