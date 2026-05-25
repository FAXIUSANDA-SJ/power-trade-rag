package com.powertrade.core.service.rag;

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
    public String extractText(byte[] fileBytes, String fileName, String contentType) {
        return "";
    }
}
