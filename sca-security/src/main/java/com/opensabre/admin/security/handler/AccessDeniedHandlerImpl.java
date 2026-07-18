package com.opensabre.admin.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.common.exception.SystemErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 权限不足处理器 - 返回403禁止访问
 */
@Slf4j
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("权限不足: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        Result<?> result = Result.fail(SystemErrorType.FORBIDDEN);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}
