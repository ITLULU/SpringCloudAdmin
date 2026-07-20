package com.opensabre.admin.rpc.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建订单请求 DTO（RPC共享）
 */
@Data
public class OrderCreateRequest {

    private String userId;
    private String hotelId;
    private String tripId;
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        private String productId;
        private String specId;
        private String productName;
        private String specName;
        private Integer quantity;
        private BigDecimal price;
    }
}
