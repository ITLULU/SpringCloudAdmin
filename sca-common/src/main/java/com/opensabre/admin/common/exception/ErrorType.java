package com.opensabre.admin.common.exception;

/**
 * 错误类型接口
 */
public interface ErrorType {
    /**
     * 返回错误码
     */
    String getCode();

    /**
     * 返回错误信息
     */
    String getMesg();
}
