package com.opensabre.admin.es.repository;

import com.opensabre.admin.es.document.OrderDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 订单 ES 文档 Repository
 */
@Repository
public interface OrderDocumentRepository extends ElasticsearchRepository<OrderDocument, String> {

    /**
     * 根据订单ID查询
     */
    Optional<OrderDocument> findByOrderId(String orderId);
}
