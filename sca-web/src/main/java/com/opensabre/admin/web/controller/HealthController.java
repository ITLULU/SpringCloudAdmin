package com.opensabre.admin.web.controller;

import com.opensabre.admin.common.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查Controller
 */
@Tag(name = "健康检查", description = "系统状态相关接口")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "健康检查", description = "检查服务运行状态")
    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> info = new HashMap<>();
        info.put("status", "UP");
        info.put("service", "sca-web");
        info.put("timestamp", LocalDateTime.now().toString());
        return info;
    }
}
