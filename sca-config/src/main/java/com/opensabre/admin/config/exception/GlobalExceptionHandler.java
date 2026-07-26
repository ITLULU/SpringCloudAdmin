package com.opensabre.admin.config.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.opensabre.admin.common.entity.Result;
import com.opensabre.admin.common.exception.BaseException;
import com.opensabre.admin.common.exception.SystemErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * 统一全局异常处理器
 * <p>
 * 集中处理所有微服务的通用异常：
 * 1. 参数校验、请求方式不支持、文件上传过大等 Spring Web 异常
 * 2. 业务自定义 BaseException
 * 3. Sentinel 限流/熔断异常（BlockException 及其 AOP 包装 UndeclaredThrowableException）
 * 4. 兜底 Exception/Throwable
 * <p>
 * 设计为最高优先级，确保在任何模块自己的 @RestControllerAdvice 之前生效。
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    // ========== Spring Web 常见异常 ==========

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> missingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.error("missing servlet request parameter exception: {}", ex.getMessage());
        return Result.fail(SystemErrorType.ARGUMENT_NOT_VALID);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> argumentInvalidException(MethodArgumentNotValidException ex) {
        log.error("service exception: {}", ex.getMessage());
        return Result.fail(SystemErrorType.ARGUMENT_NOT_VALID,
                ex.getBindingResult().getFieldError() != null
                        ? ex.getBindingResult().getFieldError().getDefaultMessage()
                        : null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> httpMessageConvertException(HttpMessageNotReadableException ex) {
        log.error("http message convert exception: {}", ex.getMessage());
        return Result.fail(SystemErrorType.ARGUMENT_NOT_VALID, "数据解析错误：" + ex.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    public Result<?> uploadFileLimitException(MultipartException ex) {
        log.error("upload file size limit: {}", ex.getMessage());
        return Result.fail(SystemErrorType.UPLOAD_FILE_SIZE_LIMIT);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> notSupportedMethodException(HttpRequestMethodNotSupportedException ex) {
        log.error("http request method not supported exception: {}", ex.getMessage());
        return Result.fail(SystemErrorType.METHOD_NOT_SUPPORTED);
    }

    // ========== 业务异常 ==========

    @ExceptionHandler(BaseException.class)
    public Result<?> baseException(BaseException ex) {
        log.error("base exception: {}", ex.getMessage());
        return Result.fail(ex.getErrorType());
    }

    // ========== Sentinel 限流/熔断异常 ==========

    /**
     * 直接抛出的 BlockException（FlowException / DegradeException / ParamFlowException 等）
     */
    @ExceptionHandler(BlockException.class)
    public ResponseEntity<Result<?>> handleBlockException(BlockException ex) {
        return buildRateLimitResponse(ex);
    }

    /**
     * Spring AOP 包装后的 BlockException。
     * BlockException 是 checked 异常且业务方法未声明，代理层会包装为 UndeclaredThrowableException。
     */
    @ExceptionHandler(UndeclaredThrowableException.class)
    public ResponseEntity<Result<?>> handleUndeclaredThrowable(UndeclaredThrowableException ex) {
        Throwable cause = ex.getUndeclaredThrowable();
        if (cause instanceof BlockException blockException) {
            return buildRateLimitResponse(blockException);
        }
        // 非 Sentinel 异常，保持兜底行为
        log.error("exception: ", cause != null ? cause : ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.fail());
    }

    private ResponseEntity<Result<?>> buildRateLimitResponse(BlockException ex) {
        String resource = ex.getRule() != null ? ex.getRule().getResource() : "unknown";
        String type = ex instanceof DegradeException ? "熔断降级" : "限流";
        log.warn("[Sentinel] 资源被{}: resource={}, rule={}", type, resource, ex.getRule());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Result.fail(SystemErrorType.RATE_LIMIT));
    }

    // ========== 兜底异常 ==========

    @ExceptionHandler({Exception.class, Throwable.class})
    public ResponseEntity<Result<?>> exception(Throwable ex) {
        log.error("exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.fail());
    }
}
