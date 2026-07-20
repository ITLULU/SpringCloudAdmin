package com.opensabre.admin.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单详情响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单详情响应")
public class OrderDetailResponse {

    @Schema(description = "订单ID")
    private String id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "酒店ID")
    private String hotelId;

    @Schema(description = "行程ID")
    private String tripId;

    @Schema(description = "订单状态：0-已取消 1-已完成")
    private Integer status;

    @Schema(description = "创建时间")
    private Date createdTime;

    @Schema(description = "订单明细列表")
    private List<ItemDetail> items;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单明细项")
    public static class ItemDetail {
        @Schema(description = "商品ID")
        private String productId;
        @Schema(description = "规格ID")
        private String specId;
        @Schema(description = "商品名称")
        private String productName;
        @Schema(description = "规格名称")
        private String specName;
        @Schema(description = "数量")
        private Integer quantity;
        @Schema(description = "单价")
        private BigDecimal price;
    }
}
