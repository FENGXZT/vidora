package com.fxzt.vidora.exception;

public class UploadException extends RuntimeException {
    private final int code;

    public UploadException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}