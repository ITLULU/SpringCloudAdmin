//package com.opensabre.admin.common.filter;
//
//import io.seata.core.context.RootContext;
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//
//import java.io.IOException;
//
///**
// * TCC 路径排除 AT 代理过滤器
// * <p>
// * 仅对下游服务的 TCC 分支接口（/inner/xxx/tcc/）生效，
// * 在 @Transactional 代理开启前 unbind Seata XID，
// * 防止 AT 模式的数据源代理拦截 SQL 并写入 undo_log。
// * <p>
// * 不影响 TM 端（sca-web）的 GlobalTransaction 编排。
// */
//@Slf4j
//@Order(Ordered.LOWEST_PRECEDENCE - 10)
//public class TccAtExcludeFilter implements Filter {
//
//    private static final String TCC_INNER_PATH = "/inner/";
//    private static final String TCC_KEYWORD = "/tcc";
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//        String requestURI = httpRequest.getRequestURI();
//
//        // 仅对下游服务的 TCC 分支接口生效（/inner/xxx/tcc/），不影响 TM 端编排
//        if (requestURI != null && requestURI.contains(TCC_INNER_PATH) && requestURI.contains(TCC_KEYWORD)) {
//            String xid = RootContext.unbind();
//            if (xid != null) {
//                log.info("[TCC-Filter] 路径 {} 脱离AT代理, unbind xid={}", requestURI, xid);
//            }
//            try {
//                chain.doFilter(request, response);
//            } finally {
//                // 恢复 XID（供后续可能的 Feign 调用使用）
//                if (xid != null) {
//                    RootContext.bind(xid);
//                }
//            }
//        } else {
//            chain.doFilter(request, response);
//        }
//    }
//}
