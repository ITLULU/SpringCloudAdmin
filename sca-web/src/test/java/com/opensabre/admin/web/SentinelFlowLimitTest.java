package com.opensabre.admin.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sentinel 限流接口 E2E 测试（URL 资源方案）
 * <p>
 * 前置条件：
 * 1. sca-web 服务已启动（默认 http://localhost:8080，可通过 -Dsentinel.test.baseUrl 覆盖）
 * 2. Nacos 中 SENTINEL_GROUP / sca-web-sentinel-flow 已配置以下规则：
 * <pre>
 * [
 *   {"resource":"/api/sentinel/test/flow","grade":1,"count":5},
 *   {"resource":"/api/sentinel/test/flow/strict","grade":1,"count":1}
 * ]
 * </pre>
 * <p>
 * 限流触发时 SentinelWebInterceptor 的 BlockExceptionHandler 返回 HTTP 429 + {"code":1010,...}
 * <p>
 * 说明：服务未启动时测试自动跳过（Assumptions），不阻塞正常构建。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SentinelFlowLimitTest {

    private static final String BASE_URL = System.getProperty("sentinel.test.baseUrl", "http://localhost:8080");
    private static final String FLOW_URL = BASE_URL + "/api/sentinel/test/flow";
    private static final String STRICT_URL = BASE_URL + "/api/sentinel/test/flow/strict";
    private static final String RESET_URL = BASE_URL + "/api/sentinel/test/reset";
    private static final String STATS_URL = BASE_URL + "/api/sentinel/test/stats";

    /** 限流响应码（SystemErrorType.RATE_LIMIT） */
    private static final String RATE_LIMIT_CODE = "1010";
    /** 限流 HTTP 状态码 */
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void checkServiceAlive() {
        boolean alive;
        try {
            HttpResponse<String> resp = get(STATS_URL);
            alive = resp.statusCode() == 200;
        } catch (Exception e) {
            alive = false;
        }
        Assumptions.assumeTrue(alive,
                "sca-web 服务未启动（" + BASE_URL + "），跳过限流 E2E 测试");
    }

    @BeforeEach
    void resetCounter() throws Exception {
        get(RESET_URL);
    }

    @Test
    @Order(1)
    @DisplayName("单次请求正常通过，不触发限流")
    void singleRequestShouldPass() throws Exception {
        HttpResponse<String> resp = get(FLOW_URL);
        System.out.println("[Test] 单次请求响应: status=" + resp.statusCode() + ", body=" + resp.body());

        assertEquals(200, resp.statusCode(), "单次请求不应被限流");
    }

    @Test
    @Order(2)
    @DisplayName("突发请求超过 QPS=5，触发限流并返回 429/1010")
    void burstRequestsShouldBeBlocked() throws Exception {
        int total = 20;
        int passed = 0;
        int blocked = 0;
        String blockedBody = null;
        int blockedStatus = 0;

        // 1 秒内快速连续请求 20 次，规则 QPS=5，必有请求被限流
        for (int i = 1; i <= total; i++) {
            HttpResponse<String> resp = get(FLOW_URL);
            if (resp.statusCode() == 200) {
                passed++;
            } else {
                blocked++;
                blockedStatus = resp.statusCode();
                blockedBody = resp.body();
            }
            System.out.printf("[Test] 第%2d次请求: status=%d%n", i, resp.statusCode());
        }

        System.out.println("=====================================");
        System.out.println("[Test] 突发测试结果: 总数=" + total + ", 通过=" + passed + ", 被限流=" + blocked);
        System.out.println("[Test] 限流响应示例: status=" + blockedStatus + ", body=" + blockedBody);
        System.out.println("=====================================");

        assertTrue(blocked > 0, "20 次突发请求（QPS=5 规则）应至少有 1 次被限流，实际 blocked=0，" +
                "请检查：① Nacos 规则 resource=testFlow 是否存在 ② eager 是否为 true ③ 启动日志 [Sentinel] 已加载限流规则");
        assertEquals(HTTP_TOO_MANY_REQUESTS, blockedStatus, "限流响应 HTTP 状态码应为 429");

        JsonNode json = MAPPER.readTree(blockedBody);
        assertEquals(RATE_LIMIT_CODE, json.get("code").asText(), "限流响应 code 应为 1010（RATE_LIMIT）");
    }

    @Test
    @Order(3)
    @DisplayName("严格限流 QPS=1，第二次请求即被限流")
    void strictFlowShouldBlockSecondRequest() throws Exception {
        HttpResponse<String> first = get(STRICT_URL);
        HttpResponse<String> second = get(STRICT_URL);

        System.out.println("[Test] 严格限流 第1次: status=" + first.statusCode());
        System.out.println("[Test] 严格限流 第2次: status=" + second.statusCode() + ", body=" + second.body());

        assertEquals(200, first.statusCode(), "第 1 次请求应通过");
        assertEquals(HTTP_TOO_MANY_REQUESTS, second.statusCode(),
                "QPS=1 规则下，同一秒内第 2 次请求应被限流（429）");
    }

    @Test
    @Order(4)
    @DisplayName("并发请求验证限流：30 并发只放行约 QPS=5")
    void concurrentRequestsShouldBeLimited() throws Exception {
        int threads = 30;
        AtomicInteger passed = new AtomicInteger();
        AtomicInteger blocked = new AtomicInteger();
        List<String> errors = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await(); // 所有线程同时发起请求
                    HttpResponse<String> resp = get(FLOW_URL);
                    if (resp.statusCode() == 200) {
                        passed.incrementAndGet();
                    } else if (resp.statusCode() == HTTP_TOO_MANY_REQUESTS) {
                        blocked.incrementAndGet();
                    } else {
                        synchronized (errors) {
                            errors.add("非预期状态码: " + resp.statusCode());
                        }
                    }
                } catch (Exception e) {
                    synchronized (errors) {
                        errors.add(e.getMessage());
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "并发请求未在 30 秒内完成");
        pool.shutdown();

        System.out.println("=====================================");
        System.out.println("[Test] 并发测试结果: 并发数=" + threads
                + ", 通过=" + passed.get() + ", 被限流=" + blocked.get()
                + ", 异常=" + errors);
        System.out.println("=====================================");

        assertTrue(errors.isEmpty(), "存在非限流异常: " + errors);
        assertTrue(blocked.get() > 0, "30 并发（QPS=5 规则）应有请求被限流");
        assertTrue(passed.get() <= 10, "QPS=5 规则下通过数不应远超阈值（允许时间窗口边界翻倍），实际通过=" + passed.get());
    }

    private static HttpResponse<String> get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
