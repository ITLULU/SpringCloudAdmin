package com.opensabre.admin.rpc.client.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量归还库存请求 DTO（RPC共享）
 */
@Data
public class StockRestoreRequest {

    private List<RestoreItem> items;

    @Data
    public static class RestoreItem {
        private String specId;
        private Integer quantity;
    }
}
