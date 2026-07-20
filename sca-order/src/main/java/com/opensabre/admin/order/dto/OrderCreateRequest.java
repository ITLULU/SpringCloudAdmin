package com.opensabre.admin.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求 DTO
 */
@Data
@Schema(description = "创建订单请求")
public class OrderCreateRequest {

    @NotBlank(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private String userId;

    @NotBlank(message = "酒店ID不能为空")
    @Schema(description = "酒店ID")
    private String hotelId;

    @NotBlank(message = "行程ID不能为空")
    @Schema(description = "行程ID")
    private String tripId;

    @NotEmpty(message = "订单商品不能为空")
    @Valid
    @Schema(description = "订单商品列表")
    private List<OrderItemRequest> items;

    /**
     * 订单商品项请求
     */
    @Data
    @Schema(description = "订单商品项")
    public static class OrderItemRequest {

        @NotBlank(message = "商品ID不能为空")
        @Schema(description = "商品ID")
        private String productId;

        @NotBlank(message = "规格ID不能为空")
        @Schema(description = "规格ID")
        private String specId;

        @NotBlank(message = "商品名称不能为空")
        @Schema(description = "商品名称")
        private String productName;

        @NotBlank(message = "规格名称不能为空")
        @Schema(description = "规格名称")
        private String specName;

        @Min(value = 1, message = "数量至少为1")
        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "单价")
        private java.math.BigDecimal price;
    }
}
