package com.opensabre.admin.es.dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.opensabre.admin.es.document.OrderDocument;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * 订单 ES 文档 DAO
 * <p>
 * 基于官方 Elasticsearch Java API Client（ElasticsearchClient，RestHighLevelClient 在 8.x
 * 的官方继任者）实现，替代原 Spring Data 的 OrderDocumentRepository：
 * <ul>
 *   <li>强类型 Lambda 构建器，请求/响应模型与 ES 8 REST API 一一对应；</li>
 *   <li>索引不存在时按 es/order-index-setting.json + es/order-index-mapping.json 创建
 *       （原先由 Spring Data 根据注解自动建索引，改用客户端后需自行管理）；</li>
 *   <li>IOException 统一包装为 UncheckedIOException，上层 Kafka 监听器捕获后不 ack、触发重试。</li>
 * </ul>
 */
@Slf4j
public class OrderDocumentDao {

    /** 索引名，与 OrderDocument @Document 注解保持一致 */
    public static final String INDEX_NAME = "order_index";

    private final ElasticsearchClient client;

    private volatile boolean indexChecked = false;

    public OrderDocumentDao(ElasticsearchClient client) {
        this.client = client;
    }

    /**
     * 保存/覆盖订单文档（以文档 id 幂等写入）
     */
    public void save(OrderDocument document) {
        ensureIndex();
        try {
            client.index(i -> i
                    .index(INDEX_NAME)
                    .id(document.getId())
                    .document(document));
        } catch (IOException e) {
            throw new UncheckedIOException("ES 保存订单文档失败: id=" + document.getId(), e);
        }
    }

    /**
     * 根据订单ID精确查询（orderId 为 keyword 字段，term 查询）
     */
    public Optional<OrderDocument> findByOrderId(String orderId) {
        ensureIndex();
        try {
            SearchResponse<OrderDocument> response = client.search(s -> s
                            .index(INDEX_NAME)
                            .size(1)
                            .query(q -> q.term(t -> t.field("orderId").value(orderId))),
                    OrderDocument.class);
            return response.hits().hits().stream()
                    .findFirst()
                    .map(Hit::source);
        } catch (IOException e) {
            throw new UncheckedIOException("ES 查询订单文档失败: orderId=" + orderId, e);
        }
    }

    /**
     * 根据订单ID删除（文档 id 即订单ID，文档不存在时静默返回，天然幂等）
     */
    public void deleteByOrderId(String orderId) {
        ensureIndex();
        try {
            client.delete(d -> d.index(INDEX_NAME).id(orderId));
        } catch (IOException e) {
            throw new UncheckedIOException("ES 删除订单文档失败: orderId=" + orderId, e);
        }
    }

    /**
     * 索引不存在时按 setting + mapping 创建（首次访问时懒加载检查，避免启动强依赖 ES）
     */
    private void ensureIndex() {
        if (indexChecked) {
            return;
        }
        synchronized (this) {
            if (indexChecked) {
                return;
            }
            try {
                boolean exists = client.indices().exists(e -> e.index(INDEX_NAME)).value();
                if (!exists) {
                    try (InputStream settings = readResource("es/order-index-setting.json");
                         InputStream mapping = readResource("es/order-index-mapping.json")) {
                        client.indices().create(c -> c
                                .index(INDEX_NAME)
                                .settings(s -> s.withJson(settings))
                                .mappings(m -> m.withJson(mapping)));
                    }
                    log.info("[ES] 已创建索引: {}", INDEX_NAME);
                }
                indexChecked = true;
            } catch (IOException e) {
                throw new UncheckedIOException("ES 索引检查/创建失败: " + INDEX_NAME, e);
            }
        }
    }

    private InputStream readResource(String path) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IOException("classpath 资源不存在: " + path);
        }
        return in;
    }
}
