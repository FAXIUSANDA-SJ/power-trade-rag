package com.powertrade.common.exception;

public class IngestTaskException extends RagException {

    private static final long serialVersionUID = 1L;

    private final IngestTaskErrorType errorType;
    private final boolean autoRetryable;

    public IngestTaskException(IngestTaskErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.autoRetryable = errorType != null && errorType.isAutoRetryable();
    }

    public IngestTaskException(IngestTaskErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.autoRetryable = errorType != null && errorType.isAutoRetryable();
    }

    public IngestTaskErrorType getErrorType() {
        return errorType;
    }

    public String getErrorCode() {
        return errorType == null ? IngestTaskErrorType.SYSTEM_ERROR.getCode() : errorType.getCode();
    }

    public boolean isAutoRetryable() {
        return autoRetryable;
    }
}
