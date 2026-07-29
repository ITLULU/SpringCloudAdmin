package com.opensabre.admin.web.controller.hotel;

import com.opensabre.admin.common.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sentinel 限流测试 Controller（URL 资源限流方案）
 * <p>
 * 不使用 @SentinelResource 注解，所有请求 URL 自动成为 Sentinel 资源。
 * 在 Nacos 中按 URL 路径配置限流规则即可。
 */
@Slf4j
@RestController
@RequestMapping("/api/sentinel/test")
@Tag(name = "Sentinel限流测试", description = "验证限流规则是否生效")
public class SentinelTestController {

    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicInteger blockCount = new AtomicInteger(0);

    /**
     * 测试接口 - URL 资源: /api/sentinel/test/flow
     * <p>
     * 在 Nacos 中配置限流规则:
     * data-id: sca-web-sentinel-flow
     * group: SENTINEL_GROUP
     * 内容: [{"resource":"/api/sentinel/test/flow","grade":1,"count":5}]
     * <p>
     * 快速连续请求超过 5 次/秒，第 6 次开始会被限流
     */
    @GetMapping("/flow")
    @Operation(summary = "测试限流", description = "QPS限制5次/秒，快速连续请求验证限流效果")
    public Result<Object> testFlow() {
        int count = requestCount.incrementAndGet();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        log.info("[SentinelTest] 请求通过, 第{}次, 时间={}", count, time);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "请求成功");
        data.put("requestCount", count);
        data.put("blockCount", blockCount.get());
        data.put("time", time);
        return Result.success(data);
    }

    /**
     * 测试接口 - URL 资源: /api/sentinel/test/flow/strict
     * <p>
     * 更严格的限流规则，QPS=1，方便测试
     */
    @GetMapping("/flow/strict")
    @Operation(summary = "严格限流测试", description = "QPS限制1次/秒，更容易触发限流")
    public Result<Object> testFlowStrict() {
        int count = requestCount.incrementAndGet();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        log.info("[SentinelTest] 严格限流接口通过, 第{}次, 时间={}", count, time);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "严格限流接口请求成功");
        data.put("requestCount", count);
        data.put("time", time);
        return Result.success(data);
    }

    /**
     * 测试接口 - URL 资源: /api/sentinel/test/auth（授权规则/来源访问控制）
     * <p>
     * 在 Nacos 中配置授权规则:
     * data-id: sca-web-sentinel-authority
     * group: SENTINEL_GROUP
     * 内容: [{"resource":"/api/sentinel/test/auth","limitApp":"sc-web","strategy":0}]
     * <p>
     * 验证方式:
     * 携带请求头 originSource: sc-web → 请求成功；
     * 不带请求头或其它值（如 originSource: postman）→ 403 + code 1005
     */
    @GetMapping("/auth")
    @Operation(summary = "测试授权规则", description = "仅请求头 originSource=sc-web 的请求可访问，其它来源返回403")
    public Result<Object> testAuthority() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        log.info("[SentinelTest] 授权接口请求通过, 时间={}", time);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "来源校验通过，请求成功");
        data.put("time", time);
        return Result.success(data);
    }

    /**
     * 重置计数器
     */
    @GetMapping("/reset")
    @Operation(summary = "重置计数器", description = "重置请求计数，方便重新测试")
    public Result<Object> reset() {
        requestCount.set(0);
        blockCount.set(0);
        log.info("[SentinelTest] 计数器已重置");
        return Result.success("计数器已重置");
    }

    /**
     * 查看当前统计
     */
    @GetMapping("/stats")
    @Operation(summary = "查看统计", description = "查看当前请求统计信息")
    public Result<Object> stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalRequests", requestCount.get());
        data.put("blockedRequests", blockCount.get());
        data.put("passRate", requestCount.get() == 0 ? "N/A" :
                String.format("%.2f%%", (1 - (double) blockCount.get() / requestCount.get()) * 100));
        return Result.success(data);
    }
}
