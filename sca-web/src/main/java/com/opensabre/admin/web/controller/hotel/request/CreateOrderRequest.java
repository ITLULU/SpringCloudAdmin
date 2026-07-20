package com.opensabre.admin.web.controller.hotel.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求
 */
@Data
@Schema(description = "创建订单请求")
public class CreateOrderRequest {

    @NotBlank(message = "酒店ID不能为空")
    @Schema(description = "酒店ID", required = true)
    private String hotelId;

    @NotBlank(message = "行程ID不能为空")
    @Schema(description = "行程ID", required = true)
    private String tripId;

    @NotEmpty(message = "订单商品不能为空")
    @Valid
    @Schema(description = "商品明细列表", required = true)
    private List<OrderItemRequest> items;

    @Data
    @Schema(description = "订单商品明细")
    public static class OrderItemRequest {

        @NotBlank(message = "商品ID不能为空")
        @Schema(description = "商品ID", required = true)
        private String productId;

        @NotBlank(message = "规格ID不能为空")
        @Schema(description = "规格ID", required = true)
        private String specId;

        @Min(value = 1, message = "数量至少为1")
        @Schema(description = "购买数量", required = true)
        private Integer quantity;
    }
}
