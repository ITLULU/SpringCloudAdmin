package com.opensabre.admin.rpc.client.dto;

import lombok.Data;

import java.util.List;

/**
 * TCC 冻结库存请求 DTO（RPC共享）
 * XID 由 Seata 自动传播，无需业务层传递
 */
@Data
public class StockFreezeRequest {

    private List<FreezeItem> items;

    @Data
    public static class FreezeItem {
        private String specId;
        private Integer quantity;
    }
}
