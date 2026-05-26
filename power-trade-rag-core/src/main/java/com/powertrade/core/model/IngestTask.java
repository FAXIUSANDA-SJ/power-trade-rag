package com.powertrade.core.model;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@Data
public class IngestTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;
    private String docId;
    private String kbId;
    private String taskType;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private String errorType;
    private String errorCode;
    private String errorMessage;
    private IngestTaskErrorDetail errorDetail;
    private Long retryDelayMs;
    private Date nextRetryTime;
    private Boolean retryReady;
    private Boolean autoRetryAllowed;
    private String retryBlockedReason;
    private Date createTime;
    private Date updateTime;
}
