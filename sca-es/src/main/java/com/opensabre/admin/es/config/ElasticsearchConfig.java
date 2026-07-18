package com.opensabre.admin.es.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch自动配置
 */
@AutoConfiguration
@EnableElasticsearchRepositories(basePackages = "com.opensabre.admin.es.repository")
public class ElasticsearchConfig {
}
