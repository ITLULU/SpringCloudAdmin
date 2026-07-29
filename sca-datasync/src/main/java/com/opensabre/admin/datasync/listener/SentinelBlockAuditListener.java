package com.opensabre.admin.datasync.listener;

import com.opensabre.admin.common.message.BaseMessage;
import com.opensabre.admin.common.message.EventType;
import com.opensabre.admin.common.message.SentinelBlockMessage;
import com.opensabre.admin.es.document.SentinelBlockDocument;
import com.opensabre.admin.es.repository.SentinelBlockDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Sentinel 拦截审计 Kafka 监听器
 * <p>
 * 消费 sca-web BlockAuditSlot 发送的拦截审计事件，直接写入 ES（sentinel_block_index）。
 * 以 messageId 作为文档 ID，重复消费天然幂等。
 */
@Slf4j
@Component
public class SentinelBlockAuditListener {

    @Autowired
    private SentinelBlockDocumentRepository sentinelBlockDocumentRepository;

    /**
     * 监听 Sentinel 拦截审计事件
     */
    @KafkaListener(topics = "${kafka.topic.sentinel-block:topic-sentinel-block}",
            groupId = "${spring.kafka.consumer.group-id:sca-datasync-group}")
    public void onSentinelBlock(@Payload BaseMessage<SentinelBlockMessage> message, Acknowledgment acknowledgment) {
        try {
            SentinelBlockMessage data = message.getData();
            if (!EventType.SENTINEL_BLOCK.getCode().equals(message.getEventType()) || data == null) {
                log.warn("[数据同步] 非法审计消息，跳过: messageId={}", message.getMessageId());
                acknowledgment.acknowledge();
                return;
            }

            SentinelBlockDocument document = new SentinelBlockDocument();
            document.setId(message.getMessageId());
            document.setApp(data.getApp());
            document.setResource(data.getResource());
            document.setBlockType(data.getBlockType());
            document.setUsername(data.getUsername());
            document.setOrigin(data.getOrigin());
            document.setRuleInfo(data.getRuleInfo());
            document.setBlockTime(toLocalDateTime(data.getBlockTime()));
            document.setCreatedTime(LocalDateTime.now());

            sentinelBlockDocumentRepository.save(document);
            acknowledgment.acknowledge();
            log.info("[数据同步] 拦截审计已写入 ES: resource={}, blockType={}, user={}, origin={}",
                    data.getResource(), data.getBlockType(), data.getUsername(), data.getOrigin());
        } catch (Exception e) {
            log.error("[数据同步] 拦截审计消息处理失败: messageId={}", message.getMessageId(), e);
            // 手动确认模式下不 ack，消息会重新消费；messageId 作为文档ID保证幂等
            throw e;
        }
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
