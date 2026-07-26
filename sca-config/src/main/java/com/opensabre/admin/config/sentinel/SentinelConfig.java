package com.opensabre.admin.config.sentinel;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.UrlCleaner;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.common.exception.SystemErrorType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Sentinel 全局配置（URL 资源统一限流方案）
 * <p>
 * 限流方式：不使用 @SentinelResource 注解，所有接口的 URL 自动成为 Sentinel 资源
 * （由 spring-cloud-starter-alibaba-sentinel 自动注册的 SentinelWebInterceptor 完成），
 * 只需在 Nacos 规则中把 resource 写成 URL 路径即可，例如：
 * <pre>
 * [{"resource":"/api/hotel/order","grade":1,"count":10}]
 * </pre>
 * <p>
 * 注意：
 * 1. 此处只声明 BlockExceptionHandler / UrlCleaner 两个 Bean，
 *    由 starter 的 SentinelWebAutoConfiguration 自动注入到它注册的拦截器中；
 *    切勿再手动 addInterceptors 注册 SentinelWebInterceptor，否则同一请求被拦截两次，QPS 统计翻倍。
 * 2. 限流/熔断规则全部由 Nacos 动态数据源推送（bootstrap.yml 中 datasource.flow/degrade）。
 */
@Slf4j
@Configuration
public class SentinelConfig  implements WebMvcConfigurer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * RESTful 路径变量匹配：纯数字 或 UUID 格式（32位十六进制含连字符）
     */
    private static final Pattern PATH_VAR_PATTERN = Pattern.compile("^\\d+$|^[0-9a-fA-F\\-]{32,36}$");

    /**
     * 启动时打印已加载的 Sentinel 规则（从 Nacos 动态数据源拉取后）
     */
    @PostConstruct
    public void printLoadedRules() {
        // 延迟一点打印，确保 Nacos 数据源已加载
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            List<FlowRule> flowRules = FlowRuleManager.getRules();
            List<DegradeRule> degradeRules = DegradeRuleManager.getRules();
            log.info("[Sentinel] 已加载限流规则 {} 条:", flowRules.size());
            for (FlowRule rule : flowRules) {
                log.info("  [Flow] resource={}, grade={}, count={}, strategy={}",
                        rule.getResource(), rule.getGrade(), rule.getCount(), rule.getStrategy());
            }
            log.info("[Sentinel] 已加载熔断规则 {} 条:", degradeRules.size());
            for (DegradeRule rule : degradeRules) {
                log.info("  [Degrade] resource={}, grade={}, count={}, slowRatio={}, timeWindow={}",
                        rule.getResource(), rule.getGrade(), rule.getCount(),
                        rule.getSlowRatioThreshold(), rule.getTimeWindow());
            }
        }).start();
    }

    /**
     * 全局 BlockExceptionHandler：URL 资源被限流/熔断时统一返回 429 + code 1010
     * <p>
     * 由 SentinelWebAutoConfiguration 自动注入到 SentinelWebInterceptor
     */
    @Bean
    public BlockExceptionHandler sentinelBlockExceptionHandler() {
        return (request, response, ex) -> {
            log.warn("[Sentinel] URL资源被限流/熔断: uri={}, rule={}", request.getRequestURI(), ex.getRule());
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(SystemErrorType.RATE_LIMIT)));
        };
    }

    /**
     * 全局 UrlCleaner：RESTful 路径变量归一化
     * <p>
     * 例: /api/hotel/order/12345 → /api/hotel/order/{id}，使同一类 URL 共享限流规则。
     * Nacos 规则中的 resource 需写清洗后的路径（即带 {id} 的形式）。
     */
    @Bean
    public UrlCleaner sentinelUrlCleaner() {
        return originUrl -> {
            if (originUrl == null || originUrl.isEmpty()) {
                return originUrl;
            }
            String[] parts = originUrl.split("/");
            StringBuilder cleaned = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    cleaned.append("/");
                }
                if (!parts[i].isEmpty() && PATH_VAR_PATTERN.matcher(parts[i]).matches()) {
                    cleaned.append("{id}");
                } else {
                    cleaned.append(parts[i]);
                }
            }
            return cleaned.toString();
        };
    }

//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        SentinelWebMvcConfig config = new SentinelWebMvcConfig();
//
//        // ========== 全局 BlockExceptionHandler ==========
//        config.setBlockExceptionHandler(new BlockExceptionHandler() {
//            @Override
//            public void handle(HttpServletRequest request, HttpServletResponse response,
//                               BlockException ex) throws Exception {
//                log.warn("[Sentinel] 资源被限流/熔断: rule={}", ex.getRule());
//
//                response.setStatus(429);
//                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//                response.setCharacterEncoding("UTF-8");
//                response.getWriter().write(objectMapper.writeValueAsString(Result.fail(SystemErrorType.RATE_LIMIT)));
//            }
//        });
//
//        // ========== 全局 UrlCleaner：RESTful 路径变量归一化 ==========
//        config.setUrlCleaner(new UrlCleaner() {
//            @Override
//            public String clean(String originUrl) {
//                if (originUrl == null || originUrl.isEmpty()) {
//                    return originUrl;
//                }
//                // 将路径中的动态参数替换为 {id}，使同一类 URL 共享限流规则
//                // 例: /api/hotel/order/12345 → /api/hotel/order/{id}
//                String[] parts = originUrl.split("/");
//                StringBuilder cleaned = new StringBuilder();
//                for (int i = 0; i < parts.length; i++) {
//                    if (i > 0) {
//                        cleaned.append("/");
//                    }
//                    if (!parts[i].isEmpty() && PATH_VAR_PATTERN.matcher(parts[i]).matches()) {
//                        cleaned.append("{id}");
//                    } else {
//                        cleaned.append(parts[i]);
//                    }
//                }
//                return cleaned.toString();
//            }
//        });
//
//        registry.addInterceptor(new SentinelWebInterceptor(config)).addPathPatterns("/**");
//    }

}
