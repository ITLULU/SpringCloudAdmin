package com.opensabre.admin.common.message;

import lombok.Data;

import java.io.Serializable;

/**
 * Kafka 消息基础实体
 * <p>
 * 统一消息格式：
 * <pre>
 * {
 *   "messageId": "uuid",
 *   "timestamp": 1712345678901,
 *   "eventType": "ORDER_CREATE",
 *   "data": {...}
 * }
 * </pre>
 *
 * @param <T> 消息业务数据类型
 */
@Data
public class BaseMessage<T> implements Serializable {

    /** 消息唯一标识，用于幂等去重 */
    private String messageId;

    /** 消息发送时间戳（毫秒） */
    private Long timestamp;

    /** 事件类型，如 ORDER_CREATE、ORDER_STATUS_CHANGE */
    private String eventType;

    /** 业务数据 */
    private T data;

    public BaseMessage() {
    }

    public BaseMessage(String messageId, Long timestamp, String eventType, T data) {
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.data = data;
    }
}
