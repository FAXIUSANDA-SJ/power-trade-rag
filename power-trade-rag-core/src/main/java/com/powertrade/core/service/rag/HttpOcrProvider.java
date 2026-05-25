package com.powertrade.core.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP OCR provider 预留实现。
 * 当前仅提供统一接入位，后续可替换为真实 OCR 服务调用。
 */
@Component
public class HttpOcrProvider implements OcrProvider {

    private static final Logger log = LoggerFactory.getLogger(HttpOcrProvider.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rag.document.ocr-endpoint:}")
    private String ocrEndpoint;

    @Value("${rag.document.ocr-api-key:}")
    private String ocrApiKey;

    @Value("${rag.document.ocr-response-field:text}")
    private String responseField;

    @Value("${rag.document.ocr-connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${rag.document.ocr-read-timeout-ms:10000}")
    private int readTimeoutMs;

    @PostConstruct
    public void initRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        restTemplate.setRequestFactory(requestFactory);
    }

    @Override
    public String getProviderName() {
        return "http";
    }

    @Override
    public String extractText(byte[] fileBytes, String fileName, String contentType) {
        if (ocrEndpoint == null || ocrEndpoint.trim().isEmpty()) {
            log.warn("HTTP OCR endpoint 未配置，fileName: {}", fileName);
            return "";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (ocrApiKey != null && !ocrApiKey.trim().isEmpty()) {
                headers.set("Authorization", "Bearer " + ocrApiKey.trim());
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("fileName", fileName);
            payload.put("contentType", contentType);
            payload.put("fileBase64", Base64.getEncoder().encodeToString(fileBytes));
            payload.put("fileSize", fileBytes.length);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    ocrEndpoint,
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("HTTP OCR 调用失败，status: {}, fileName: {}", response.getStatusCode(), fileName);
                return "";
            }

            JsonNode root = objectMapper.readTree(response.getBody().getBytes(StandardCharsets.UTF_8));
            JsonNode textNode = root.path(responseField);
            if (textNode.isMissingNode() || textNode.isNull()) {
                log.warn("HTTP OCR 响应中未找到字段 {}, fileName: {}", responseField, fileName);
                return "";
            }
            return textNode.asText("");
        } catch (Exception e) {
            log.error("HTTP OCR 调用失败，fileName: {}", fileName, e);
            return "";
        }
    }
}
