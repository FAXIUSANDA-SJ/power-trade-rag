package com.powertrade.core.service.rag;

import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    String extractText(MultipartFile file);
}
