package com.opensabre.admin.order.service;

import com.opensabre.admin.common.message.BaseMessage;
import com.opensabre.admin.common.message.EventType;
import com.opensabre.admin.common.message.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 订单事件发布器
 * <p>
 * 负责将订单事件发送到 Kafka，消息体只包含订单ID，由 sca-datasync 消费后查询数据库同步到 ES。
 */
@Slf4j
@Service
public class OrderEventPublisher {

    @Value("${kafka.topic.order-create:topic-order-create}")
    private String orderCreateTopic;

    @Value("${kafka.topic.order-status-change:topic-order-status-change}")
    private String orderStatusChangeTopic;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送订单创建事件
     */
    public void publishOrderCreate(String orderId) {
        BaseMessage<OrderMessage> message = buildMessage(EventType.ORDER_CREATE, orderId);
        send(orderCreateTopic, orderId, message);
        log.info("[订单事件] 已发送订单创建事件: orderId={}, messageId={}", orderId, message.getMessageId());
    }

    /**
     * 发送订单状态变更事件
     */
    public void publishOrderStatusChange(String orderId) {
        BaseMessage<OrderMessage> message = buildMessage(EventType.ORDER_STATUS_CHANGE, orderId);
        send(orderStatusChangeTopic, orderId, message);
        log.info("[订单事件] 已发送订单状态变更事件: orderId={}, messageId={}", orderId, message.getMessageId());
    }

    private BaseMessage<OrderMessage> buildMessage(EventType eventType, String orderId) {
        OrderMessage data = new OrderMessage();
        data.setOrderId(orderId);

        BaseMessage<OrderMessage> message = new BaseMessage<>();
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setTimestamp(System.currentTimeMillis());
        message.setEventType(eventType.getCode());
        message.setData(data);
        return message;
    }

    private void send(String topic, String key, BaseMessage<OrderMessage> message) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[订单事件] Kafka 发送失败: topic={}, key={}, error={}", topic, key, ex.getMessage(), ex);
            }
        });
    }
}
