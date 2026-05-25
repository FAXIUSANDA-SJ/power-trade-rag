package com.powertrade.core.service.rag;

public interface OcrProvider {

    String getProviderName();

    String extractText(byte[] fileBytes, String fileName, String contentType);
}
