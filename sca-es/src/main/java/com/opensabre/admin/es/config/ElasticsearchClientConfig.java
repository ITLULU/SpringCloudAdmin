package com.opensabre.admin.es.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

/**
 * ElasticsearchClient 手动装配
 * <p>
 * 显式构建官方 Java API Client 的三层结构，替代 Spring Boot 自动装配
 * （所有 Bean 均带 @ConditionalOnMissingBean，Boot 自动配置检测到已存在同类 Bean 后整体退让）：
 * <pre>
 * RestClient（低层 HTTP：连接池、多节点负载均衡与故障转移、认证、超时）
 *   └─ ElasticsearchTransport（传输层：绑定 JsonpMapper 完成 JSON 编解码）
 *        └─ ElasticsearchClient（强类型 API 门面，供 DAO / Spring Data 使用）
 * </pre>
 * 配置项沿用 Boot 标准前缀 spring.elasticsearch.*，uris 支持逗号分隔多地址（集群接入）。
 */
@Slf4j
@AutoConfiguration(before = {ElasticsearchRestClientAutoConfiguration.class, ElasticsearchClientAutoConfiguration.class})
public class ElasticsearchClientConfig {

    /** ES 节点地址，逗号分隔支持集群，如 http://host:9200,http://host:9201 */
    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private List<String> uris;

    /** 用户名，未开启 xpack.security 时留空 */
    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    /** 建立连接超时 */
    @Value("${spring.elasticsearch.connection-timeout:5s}")
    private Duration connectionTimeout;

    /** 读响应超时 */
    @Value("${spring.elasticsearch.socket-timeout:30s}")
    private Duration socketTimeout;

    /**
     * 低层 RestClient：多节点时客户端内部轮询请求各节点，节点故障自动摘除并定期探活恢复
     */
    @Bean
    @ConditionalOnMissingBean
    public RestClient restClient() {
        HttpHost[] hosts = uris.stream()
                .map(String::trim)
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        RestClientBuilder builder = RestClient.builder(hosts)
                .setRequestConfigCallback(requestConfig -> requestConfig
                        .setConnectTimeout((int) connectionTimeout.toMillis())
                        .setSocketTimeout((int) socketTimeout.toMillis()));

        // 服务端开启 xpack.security 时的 Basic 认证
        if (StringUtils.hasText(username)) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClient ->
                    httpClient.setDefaultCredentialsProvider(credentialsProvider));
        }

        log.info("[ES] RestClient 已装配: nodes={}, connectTimeout={}, socketTimeout={}",
                uris, connectionTimeout, socketTimeout);
        return builder.build();
    }

    /**
     * 传输层：绑定 ElasticsearchConfig 中定制的 JsonpMapper（LocalDateTime 格式、忽略未知字段）；
     * Bean 销毁时 Spring 调用 close() 一并关闭底层 RestClient
     */
    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient, JsonpMapper jsonpMapper) {
        return new RestClientTransport(restClient, jsonpMapper);
    }

    /**
     * 强类型客户端门面：OrderDocumentDao 与 Spring Data Repository 共用此实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
