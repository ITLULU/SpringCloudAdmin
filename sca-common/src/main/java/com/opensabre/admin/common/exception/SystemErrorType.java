package com.opensabre.admin.common.exception;

import lombok.Getter;

/**
 * 系统错误类型枚举
 */
@Getter
public enum SystemErrorType implements ErrorType {

    SYSTEM_ERROR(500, "系统繁忙,请稍后重试"),
    ARGUMENT_NOT_VALID(1001, "参数无效"),
    METHOD_NOT_SUPPORTED(1002, "请求方法不支持"),
    UPLOAD_FILE_SIZE_LIMIT(1003, "上传文件大小超过限制"),
    UNAUTHORIZED(1004, "未授权，请登录"),
    FORBIDDEN(1005, "权限不足，拒绝访问"),
    TOKEN_EXPIRED(1006, "Token已过期"),
    TOKEN_INVALID(1007, "Token无效"),
    USER_NOT_FOUND(1005, "用户不存在"),
    PASSWORD_ERROR(1006, "密码错误");

    private final Integer code;
    private final String mesg;

    SystemErrorType(Integer code, String mesg) {
        this.code = code;
        this.mesg = mesg;
    }
}
