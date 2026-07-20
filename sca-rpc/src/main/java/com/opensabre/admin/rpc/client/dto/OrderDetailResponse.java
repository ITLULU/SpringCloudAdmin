package com.opensabre.admin.rpc.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单详情响应 DTO（RPC共享）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {

    private String id;
    private String userId;
    private String hotelId;
    private String tripId;
    private Integer status;
    private Date createdTime;
    private List<ItemDetail> items;
    private BigDecimal totalAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDetail {
        private String productId;
        private String specId;
        private String productName;
        private String specName;
        private Integer quantity;
        private BigDecimal price;
    }
}
