package com.opensabre.admin.common.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用订单消息数据
 * <p>
 * 为了保持消息体通用、稳定，Kafka 中只传输订单ID。
 * 消费者（sca-datasync）根据 orderId 查询数据库获取最新完整订单信息后同步到 ES。
 */
@Data
public class OrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private String orderId;
}
