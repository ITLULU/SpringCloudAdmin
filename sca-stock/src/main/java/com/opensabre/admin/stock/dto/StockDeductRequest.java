package com.opensabre.admin.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量扣减库存请求 DTO
 */
@Data
@Schema(description = "批量扣减库存请求")
public class StockDeductRequest {

    @NotEmpty(message = "扣减明细不能为空")
    @Valid
    @Schema(description = "库存扣减明细列表")
    private List<DeductItem> items;

    @Data
    @Schema(description = "库存扣减项")
    public static class DeductItem {

        @NotBlank(message = "规格ID不能为空")
        @Schema(description = "规格ID")
        private String specId;

        @Min(value = 1, message = "扣减数量至少为1")
        @Schema(description = "扣减数量")
        private Integer quantity;
    }
}
