package com.powertrade.common.exception;

public enum IngestTaskErrorType {

    DOC_NOT_FOUND("DOC_NOT_FOUND", "DOCUMENT", false, "文档不存在"),
    OCR_PROVIDER_DISABLED("OCR_PROVIDER_DISABLED", "OCR", false, "OCR provider 未启用"),
    OCR_PROVIDER_NOT_CONFIGURED("OCR_PROVIDER_NOT_CONFIGURED", "OCR", false, "OCR endpoint 未配置"),
    OCR_FILE_TOO_LARGE("OCR_FILE_TOO_LARGE", "OCR", false, "OCR 文件超过限制"),
    OCR_RESPONSE_INVALID("OCR_RESPONSE_INVALID", "OCR", false, "OCR 响应字段无效"),
    OCR_REQUEST_FAILED("OCR_REQUEST_FAILED", "OCR", true, "OCR 远程调用失败"),
    OCR_FILE_READ_FAILED("OCR_FILE_READ_FAILED", "OCR", false, "OCR 读取文件失败"),
    PARSE_FAILED("PARSE_FAILED", "PARSE", false, "文档解析失败"),
    STORAGE_ERROR("STORAGE_ERROR", "STORAGE", true, "存储访问失败"),
    VECTOR_ERROR("VECTOR_ERROR", "VECTOR", true, "向量处理失败"),
    CONFIGURATION_ERROR("CONFIGURATION_ERROR", "CONFIG", false, "配置错误"),
    RETRY_NOT_ALLOWED("RETRY_NOT_ALLOWED", "TASK", false, "任务不允许重试"),
    SYSTEM_ERROR("SYSTEM_ERROR", "SYSTEM", true, "系统异常");

    private final String code;
    private final String category;
    private final boolean autoRetryable;
    private final String description;

    IngestTaskErrorType(String code, String category, boolean autoRetryable, String description) {
        this.code = code;
        this.category = category;
        this.autoRetryable = autoRetryable;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getCategory() {
        return category;
    }

    public boolean isAutoRetryable() {
        return autoRetryable;
    }

    public String getDescription() {
        return description;
    }
}
