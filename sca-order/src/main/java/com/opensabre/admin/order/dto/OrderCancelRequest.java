package com.opensabre.admin.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * 取消订单请求 DTO
 */
@Data
@Schema(description = "取消订单请求")
public class OrderCancelRequest {

    @NotBlank(message = "订单ID不能为空")
    @Schema(description = "订单ID")
    private String orderId;

    @NotEmpty(message = "库存归还明细不能为空")
    @Valid
    @Schema(description = "需要归还库存的明细列表")
    private List<StockRestoreItem> items;

    @Data
    @Schema(description = "库存归还项")
    public static class StockRestoreItem {

        @NotBlank(message = "规格ID不能为空")
        @Schema(description = "规格ID")
        private String specId;

        @Schema(description = "归还数量")
        private Integer quantity;
    }
}
