package com.opensabre.admin.datasync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数据同步服务启动类
 * <p>
 * 消费 Kafka 订单事件消息，同步数据到 Elasticsearch。
 */
@SpringBootApplication
public class DataSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataSyncApplication.class, args);
    }
}
