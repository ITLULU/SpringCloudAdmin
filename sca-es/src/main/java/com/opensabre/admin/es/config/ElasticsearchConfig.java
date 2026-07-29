package com.opensabre.admin.es.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.opensabre.admin.es.dao.OrderDocumentDao;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Elasticsearch自动配置
 * <p>
 * 两套访问方式并存：
 * <ul>
 *   <li>Spring Data Repository（如 SentinelBlockDocumentRepository）——简单 CRUD；</li>
 *   <li>官方 Elasticsearch Java API Client（ElasticsearchClient，RestHighLevelClient 在 8.x
 *       的官方继任者）——如 {@link OrderDocumentDao}，强类型 Lambda 构建器，支持复杂查询与聚合。</li>
 * </ul>
 * 注册在 ElasticsearchClientAutoConfiguration 之前，以便自定义 JsonpMapper 生效
 * （Boot 侧为 @ConditionalOnMissingBean）。
 */
@AutoConfiguration(before = ElasticsearchClientAutoConfiguration.class)
@EnableElasticsearchRepositories(basePackages = "com.opensabre.admin.es.repository")
public class ElasticsearchConfig {

    /** 与索引 mapping 中 date 字段 pattern（yyyy-MM-dd'T'HH:mm:ss）保持一致 */
    private static final DateTimeFormatter ES_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 定制 ElasticsearchClient 的 JSON 映射器：
     * <ul>
     *   <li>LocalDateTime 按索引 date pattern 序列化（截断纳秒），否则写入会因格式不匹配被 ES 拒绝；</li>
     *   <li>忽略未知字段——兼容历史上由 Spring Data Repository 写入的 _class 等附加字段；</li>
     *   <li>NON_NULL——空字段不写入 _source。</li>
     * </ul>
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonpMapper jsonpMapper() {
        SimpleModule javaTimeModule = new SimpleModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(ES_DATE_TIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(ES_DATE_TIME_FORMATTER));

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(javaTimeModule)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new JacksonJsonpMapper(objectMapper);
    }

    /**
     * 订单文档 DAO（基于官方 ElasticsearchClient）
     */
    @Bean
    @ConditionalOnMissingBean
    public OrderDocumentDao orderDocumentDao(ElasticsearchClient elasticsearchClient) {
        return new OrderDocumentDao(elasticsearchClient);
    }
}
