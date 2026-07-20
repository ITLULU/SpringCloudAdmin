package com.opensabre.admin.rpc.client.dto;

import lombok.Data;

import java.util.List;

/**
 * 取消订单请求 DTO（RPC共享）
 */
@Data
public class OrderCancelRequest {

    private String orderId;
    private List<StockRestoreItem> items;

    @Data
    public static class StockRestoreItem {
        private String specId;
        private Integer quantity;
    }
}
