package com.powertrade.core.service.rag;

import com.powertrade.common.exception.IngestTaskErrorType;
import com.powertrade.common.exception.IngestTaskException;
import com.powertrade.core.model.OcrExtractResult;
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
    public OcrExtractResult extract(MultipartFile file) {
        OcrProvider provider = resolveProvider();
        try {
            OcrExtractResult result = provider.extract(file.getBytes(), file.getOriginalFilename(), file.getContentType());
            if (result.getProvider() == null || result.getProvider().trim().isEmpty()) {
                result.setProvider(provider.getProviderName());
            }
            return result;
        } catch (IOException e) {
            throw new IngestTaskException(IngestTaskErrorType.OCR_FILE_READ_FAILED, "OCR 读取文件失败", e);
        }
    }

    @Override
    public String extractText(MultipartFile file) {
        return extract(file).getText();
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
                .orElseThrow(() -> new IngestTaskException(IngestTaskErrorType.CONFIGURATION_ERROR, "未找到默认 OCR provider"));
    }
}
