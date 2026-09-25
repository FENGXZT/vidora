package com.fxzt.vidora.exception;

/**
 * 自定义异常类，用于表示未授权的访问
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
