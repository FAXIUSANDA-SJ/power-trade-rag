package com.powertrade.core.service.rag;

import com.powertrade.core.model.OcrExtractResult;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    OcrExtractResult extract(MultipartFile file);

    String extractText(MultipartFile file);
}
