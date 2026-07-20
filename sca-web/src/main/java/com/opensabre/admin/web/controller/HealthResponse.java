package com.opensabre.admin.web.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 健康检查响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "健康检查响应")
public class HealthResponse {

    @Schema(description = "服务状态")
    private String status;

    @Schema(description = "服务名称")
    private String service;

    @Schema(description = "时间戳")
    private String timestamp;
}
