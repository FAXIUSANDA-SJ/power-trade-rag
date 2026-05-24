package com.powertrade.common.exception;

public class RagException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Integer code;

    public RagException(String message) {
        super(message);
        this.code = 500;
    }

    public RagException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public RagException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public Integer getCode() {
        return code;
    }
}