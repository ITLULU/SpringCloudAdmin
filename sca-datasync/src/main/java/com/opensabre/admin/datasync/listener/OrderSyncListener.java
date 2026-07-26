package com.opensabre.admin.datasync.listener;

import com.opensabre.admin.common.message.BaseMessage;
import com.opensabre.admin.common.message.EventType;
import com.opensabre.admin.common.message.OrderMessage;
import com.opensabre.admin.datasync.service.OrderSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 订单同步 Kafka 监听器
 */
@Slf4j
@Component
public class OrderSyncListener {

    @Autowired
    private OrderSyncService orderSyncService;

    /**
     * 监听订单创建事件
     */
    @KafkaListener(topics = "${kafka.topic.order-create:topic-order-create}",
            groupId = "${spring.kafka.consumer.group-id:sca-datasync-group}")
    public void onOrderCreate(@Payload BaseMessage<OrderMessage> message, Acknowledgment acknowledgment) {
        handleMessage(message, acknowledgment, EventType.ORDER_CREATE);
    }

    /**
     * 监听订单状态变更事件
     */
    @KafkaListener(topics = "${kafka.topic.order-status-change:topic-order-status-change}",
            groupId = "${spring.kafka.consumer.group-id:sca-datasync-group}")
    public void onOrderStatusChange(@Payload BaseMessage<OrderMessage> message, Acknowledgment acknowledgment) {
        handleMessage(message, acknowledgment, EventType.ORDER_STATUS_CHANGE);
    }

    private void handleMessage(BaseMessage<OrderMessage> message, Acknowledgment acknowledgment, EventType expectedType) {
        try {
            String orderId = message.getData() != null ? message.getData().getOrderId() : null;
            log.info("[数据同步] 收到订单消息: messageId={}, eventType={}, expectedType={}, orderId={}",
                    message.getMessageId(), message.getEventType(), expectedType.getCode(), orderId);

            if (!expectedType.getCode().equals(message.getEventType()) || orderId == null) {
                log.warn("[数据同步] 非法订单消息，跳过: messageId={}", message.getMessageId());
                acknowledgment.acknowledge();
                return;
            }

            orderSyncService.syncOrder(orderId);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("[数据同步] 订单消息处理失败: messageId={}", message.getMessageId(), e);
            // 手动确认模式下不 ack，消息会重新消费；可配合死信队列进一步处理
            throw e;
        }
    }
}
