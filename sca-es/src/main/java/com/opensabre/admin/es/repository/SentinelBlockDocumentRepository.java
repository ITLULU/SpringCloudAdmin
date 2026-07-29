package com.opensabre.admin.es.repository;

import com.opensabre.admin.es.document.SentinelBlockDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Sentinel 拦截审计 ES 文档 Repository
 */
@Repository
public interface SentinelBlockDocumentRepository extends ElasticsearchRepository<SentinelBlockDocument, String> {
}
