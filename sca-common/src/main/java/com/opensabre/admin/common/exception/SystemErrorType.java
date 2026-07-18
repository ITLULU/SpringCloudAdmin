package com.opensabre.admin.common.exception;

import lombok.Getter;

/**
 * 系统错误类型枚举
 */
@Getter
public enum SystemErrorType implements ErrorType {

    SYSTEM_ERROR("100000", "系统异常"),
    SYSTEM_BUSY("100001", "系统繁忙,请稍后重试"),
    ARGUMENT_NOT_VALID("100002", "参数无效"),
    METHOD_NOT_SUPPORTED("100003", "请求方法不支持"),
    UPLOAD_FILE_SIZE_LIMIT("100004", "上传文件大小超过限制"),
    UNAUTHORIZED("100005", "未授权，请登录"),
    FORBIDDEN("100006", "权限不足，拒绝访问"),
    TOKEN_EXPIRED("100007", "Token已过期"),
    TOKEN_INVALID("100008", "Token无效"),
    USER_NOT_FOUND("100009", "用户不存在"),
    PASSWORD_ERROR("100010", "密码错误");

    private final String code;
    private final String mesg;

    SystemErrorType(String code, String mesg) {
        this.code = code;
        this.mesg = mesg;
    }
}
