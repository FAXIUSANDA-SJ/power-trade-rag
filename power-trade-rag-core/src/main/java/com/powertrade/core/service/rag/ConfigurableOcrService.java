package com.powertrade.core.service.rag;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConfigurableOcrService implements OcrService {

    private final List<OcrProvider> ocrProviders;

    @Value("${rag.document.ocr-provider:none}")
    private String ocrProvider;

    public ConfigurableOcrService(List<OcrProvider> ocrProviders) {
        this.ocrProviders = ocrProviders;
    }

    @Override
    public String extractText(MultipartFile file) {
        OcrProvider provider = resolveProvider();
        try {
            return provider.extractText(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("OCR 读取文件失败", e);
        }
    }

    public String getActiveProviderName() {
        return resolveProvider().getProviderName();
    }

    private OcrProvider resolveProvider() {
        return ocrProviders.stream()
                .filter(item -> item.getProviderName().equalsIgnoreCase(ocrProvider))
                .findFirst()
                .orElseGet(this::getFallbackProvider);
    }

    private OcrProvider getFallbackProvider() {
        return ocrProviders.stream()
                .filter(item -> "none".equalsIgnoreCase(item.getProviderName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到默认 OCR provider"));
    }
}
