package com.opensabre.admin.common.exception;

/**
 * 业务异常类
 */
public class ServiceException extends BaseException {

    public ServiceException(ErrorType errorType) {
        super(errorType);
    }

    public ServiceException(ErrorType errorType, String message) {
        super(errorType, message);
    }
}
