package com.opensabre.admin.common.message;

import lombok.Getter;

/**
 * 消息事件类型枚举
 * <p>
 * 统一 Kafka 消息中的 eventType 字段取值，避免各服务硬编码字符串。
 */
@Getter
public enum EventType {

    /** 订单创建 */
    ORDER_CREATE("ORDER_CREATE", "订单创建事件"),

    /** 订单状态变更 */
    ORDER_STATUS_CHANGE("ORDER_STATUS_CHANGE", "订单状态变更事件");

    /** 事件类型编码 */
    private final String code;

    /** 事件类型描述 */
    private final String desc;

    EventType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
