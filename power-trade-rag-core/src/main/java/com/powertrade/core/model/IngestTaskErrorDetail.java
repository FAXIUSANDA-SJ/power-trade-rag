package com.powertrade.core.model;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class IngestTaskErrorDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private String category;
    private String errorType;
    private String errorCode;
    private String message;
    private String summary;
    private Integer retryCount;
    private Integer maxRetryCount;
    private boolean retryExhausted;
    private boolean canRetry;
    private boolean autoRetryAllowed;
    private Boolean retryReady;
    private Long retryDelayMs;
    private String retryBlockedReason;
    private Date nextRetryTime;
    private Date lastOccurredAt;
}
