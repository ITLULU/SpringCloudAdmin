package com.opensabre.admin.common.entity;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.opensabre.admin.common.exception.BaseException;
import com.opensabre.admin.common.exception.ErrorType;
import com.opensabre.admin.common.exception.SystemErrorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * REST统一返回数据结构
 */
@Schema(description = "REST请求的统一返回模型")
@Getter
public class Result<T> {

    public static final Integer SUCCESSFUL_CODE = 200;
    public static final String SUCCESSFUL_MESG = "success";

    @Schema(title = "处理结果code", required = true)
    private  Integer code;

    @Schema(title = "处理结果描述信息")
    private  String msg;

    @Schema(title = "请求结果生成时间戳", required = true)
    @JsonFormat(pattern = DatePattern.UTC_MS_PATTERN)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime time;

    @Schema(title = "处理结果数据信息", required = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public Result() {
        this.code = SUCCESSFUL_CODE;
        this.msg = SUCCESSFUL_MESG;
        this.time = LocalDateTime.now();
    }

    public Result(ErrorType errorType) {
        this.code = errorType.getCode();
        this.msg = errorType.getMesg();
        this.time = LocalDateTime.now();
    }

    public Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
        this.time = LocalDateTime.now();
    }

    public Result(ErrorType errorType, T data) {
        this(errorType);
        this.data = data;
    }

    private Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.time = LocalDateTime.now();
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESSFUL_CODE, SUCCESSFUL_MESG, data);
    }

    public static Result<Object> success() {
        return success(null);
    }

    public static Result<Object> fail() {
        return new Result<>(SystemErrorType.SYSTEM_ERROR);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(SystemErrorType.SYSTEM_ERROR.getCode(), message);
    }

    public static Result<Object> fail(BaseException baseException) {
        return new Result<>(baseException.getErrorType());
    }

    public static <T> Result<T> fail(ErrorType errorType, T data) {
        return new Result<>(errorType, data);
    }

    public static Result<Object> fail(ErrorType errorType) {
        return new Result<>(errorType);
    }

    @JsonIgnore
    public boolean isSuccess() {
        return SUCCESSFUL_CODE.equals(this.code);
    }

    @JsonIgnore
    public boolean isFail() {
        return !isSuccess();
    }
}
