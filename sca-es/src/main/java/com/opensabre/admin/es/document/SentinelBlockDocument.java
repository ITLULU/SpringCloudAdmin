package com.opensabre.admin.es.document;

import com.opensabre.admin.es.repository.BaseDocument;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;

/**
 * Sentinel 拦截审计 ES 文档
 * <p>
 * 索引名：sentinel_block_index
 * 记录限流/熔断/授权拦截事件（谁、什么时候、哪个接口、命中哪条规则），
 * 数据来源：sca-web BlockAuditSlot → Kafka(topic-sentinel-block) → sca-datasync 消费写入。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "sentinel_block_index")
@Setting(settingPath = "es/sentinel-block-index-setting.json")
public class SentinelBlockDocument extends BaseDocument {

    private static final long serialVersionUID = 1L;

    /** 触发拦截的服务名，如 sca-web */
    @Field(type = FieldType.Keyword)
    private String app;

    /** 被拦截的资源名（URL） */
    @Field(type = FieldType.Keyword)
    private String resource;

    /** 拦截类型：FLOW/DEGRADE/AUTHORITY/USER_FLOW/PARAM_FLOW/SYSTEM/OTHER */
    @Field(type = FieldType.Keyword)
    private String blockType;

    /** 触发拦截的登录用户名，匿名请求为空 */
    @Field(type = FieldType.Keyword)
    private String username;

    /** 调用来源（originSource 请求头解析结果） */
    @Field(type = FieldType.Keyword)
    private String origin;

    /** 命中的规则描述 */
    @Field(type = FieldType.Keyword, ignoreAbove = 2048)
    private String ruleInfo;

    /** 拦截发生时间 */
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime blockTime;
}
