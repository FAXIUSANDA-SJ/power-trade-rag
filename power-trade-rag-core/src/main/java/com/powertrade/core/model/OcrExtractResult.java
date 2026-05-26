package com.powertrade.core.model;

import java.io.Serializable;
import lombok.Data;

@Data
public class OcrExtractResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String provider;
    private boolean success;
    private String errorCode;
    private String errorType;
    private String text;
    private Integer textLength;
    private String errorMessage;
    private String responsePreview;
    private Integer httpStatus;
    private Long durationMs;
}
