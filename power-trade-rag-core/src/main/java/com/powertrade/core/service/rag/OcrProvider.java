package com.powertrade.core.service.rag;

import com.powertrade.core.model.OcrExtractResult;

public interface OcrProvider {

    String getProviderName();

    OcrExtractResult extract(byte[] fileBytes, String fileName, String contentType);
}
