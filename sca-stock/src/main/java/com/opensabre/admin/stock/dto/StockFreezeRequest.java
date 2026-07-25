package com.opensabre.admin.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * TCC 冻结库存请求 DTO
 */
@Data
@Schema(description = "TCC冻结库存请求")
public class StockFreezeRequest {

    @NotBlank(message = "全局事务ID不能为空")
    @Schema(description = "全局事务ID")
    private String xid;

    @NotEmpty(message = "冻结明细不能为空")
    @Valid
    @Schema(description = "库存冻结明细列表")
    private List<FreezeItem> items;

    @Data
    @Schema(description = "库存冻结项")
    public static class FreezeItem {

        @NotBlank(message = "规格ID不能为空")
        @Schema(description = "规格ID")
        private String specId;

        @Min(value = 1, message = "冻结数量至少为1")
        @Schema(description = "冻结数量")
        private Integer quantity;
    }
}
