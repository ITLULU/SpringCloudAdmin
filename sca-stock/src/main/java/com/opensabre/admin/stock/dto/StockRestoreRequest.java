package com.opensabre.admin.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量归还库存请求 DTO
 */
@Data
@Schema(description = "批量归还库存请求")
public class StockRestoreRequest {

    @NotEmpty(message = "归还明细不能为空")
    @Valid
    @Schema(description = "库存归还明细列表")
    private List<RestoreItem> items;

    @Data
    @Schema(description = "库存归还项")
    public static class RestoreItem {

        @NotBlank(message = "规格ID不能为空")
        @Schema(description = "规格ID")
        private String specId;

        @Min(value = 1, message = "归还数量至少为1")
        @Schema(description = "归还数量")
        private Integer quantity;
    }
}
