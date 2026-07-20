package com.opensabre.admin.web.controller.hotel.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建行程请求
 */
@Data
@Schema(description = "创建行程请求")
public class CreateTripRequest {

    @NotBlank(message = "酒店ID不能为空")
    @Schema(description = "酒店ID", required = true)
    private String hotelId;

    @NotNull(message = "入住日期不能为空")
    @Schema(description = "入住日期 yyyy-MM-dd", required = true)
    private LocalDate checkInDate;

    @NotNull(message = "离店日期不能为空")
    @Schema(description = "离店日期 yyyy-MM-dd", required = true)
    private LocalDate checkOutDate;
}
