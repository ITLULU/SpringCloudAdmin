package com.opensabre.admin.rpc.client.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量扣减库存请求 DTO（RPC共享）
 */
@Data
public class StockDeductRequest {

    private List<DeductItem> items;

    @Data
    public static class DeductItem {
        private String specId;
        private Integer quantity;
    }
}
