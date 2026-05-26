package com.powertrade.core.service.rag;

import com.powertrade.common.exception.IngestTaskErrorType;
import com.powertrade.core.model.OcrExtractResult;
import org.springframework.stereotype.Component;

/**
 * OCR 预留实现，当前仅作为接入点占位。
 */
@Component
public class NoopOcrService implements OcrProvider {

    @Override
    public String getProviderName() {
        return "none";
    }

    @Override
    public OcrExtractResult extract(byte[] fileBytes, String fileName, String contentType) {
        OcrExtractResult result = new OcrExtractResult();
        result.setProvider(getProviderName());
        result.setSuccess(false);
        result.setErrorType(IngestTaskErrorType.OCR_PROVIDER_DISABLED.name());
        result.setErrorCode(IngestTaskErrorType.OCR_PROVIDER_DISABLED.getCode());
        result.setText("");
        result.setTextLength(0);
        result.setErrorMessage("OCR provider 未启用");
        result.setDurationMs(0L);
        return result;
    }
}
