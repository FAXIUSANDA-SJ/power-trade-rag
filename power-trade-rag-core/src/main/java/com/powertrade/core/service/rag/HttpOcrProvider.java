package com.powertrade.core.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powertrade.common.exception.IngestTaskErrorType;
import com.powertrade.core.model.OcrExtractResult;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

    @Value("${rag.document.ocr-auth-header:Authorization}")
    private String authHeaderName;

    @Value("${rag.document.ocr-auth-prefix:Bearer }")
    private String authHeaderPrefix;

    @Value("${rag.document.ocr-response-field:text}")
    private String responseField;

    @Value("${rag.document.ocr-response-success-field:}")
    private String responseSuccessField;

    @Value("${rag.document.ocr-response-message-field:message}")
    private String responseMessageField;

    @Value("${rag.document.ocr-response-code-field:code}")
    private String responseCodeField;

    @Value("${rag.document.ocr-success-values:true,0,200,ok,success}")
    private String responseSuccessValues;

    @Value("${rag.document.ocr-request-file-field:fileBase64}")
    private String requestFileField;

    @Value("${rag.document.ocr-max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

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
    public OcrExtractResult extract(byte[] fileBytes, String fileName, String contentType) {
        long startTime = System.currentTimeMillis();
        OcrExtractResult result = new OcrExtractResult();
        result.setProvider(getProviderName());
        if (ocrEndpoint == null || ocrEndpoint.trim().isEmpty()) {
            log.warn("HTTP OCR endpoint 未配置，fileName: {}", fileName);
            result.setSuccess(false);
            result.setErrorType(IngestTaskErrorType.OCR_PROVIDER_NOT_CONFIGURED.name());
            result.setErrorCode(IngestTaskErrorType.OCR_PROVIDER_NOT_CONFIGURED.getCode());
            result.setText("");
            result.setTextLength(0);
            result.setErrorMessage("HTTP OCR endpoint 未配置");
            result.setDurationMs(System.currentTimeMillis() - startTime);
            return result;
        }
        if (maxFileSizeBytes > 0 && fileBytes != null && fileBytes.length > maxFileSizeBytes) {
            result.setSuccess(false);
            result.setErrorType(IngestTaskErrorType.OCR_FILE_TOO_LARGE.name());
            result.setErrorCode(IngestTaskErrorType.OCR_FILE_TOO_LARGE.getCode());
            result.setText("");
            result.setTextLength(0);
            result.setErrorMessage("OCR 文件过大，已超过限制: " + fileBytes.length + " > " + maxFileSizeBytes);
            result.setDurationMs(System.currentTimeMillis() - startTime);
            return result;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (ocrApiKey != null && !ocrApiKey.trim().isEmpty()) {
                String headerName = authHeaderName == null || authHeaderName.trim().isEmpty()
                        ? "Authorization"
                        : authHeaderName.trim();
                headers.set(headerName, buildAuthHeaderValue());
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("fileName", fileName);
            payload.put("contentType", contentType);
            payload.put(resolveRequestFileField(), Base64.getEncoder().encodeToString(fileBytes));
            payload.put("fileSize", fileBytes.length);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    ocrEndpoint,
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            result.setDurationMs(System.currentTimeMillis() - startTime);
            result.setHttpStatus(response.getStatusCodeValue());
            result.setResponsePreview(preview(response.getBody()));
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("HTTP OCR 调用失败，status: {}, fileName: {}", response.getStatusCode(), fileName);
                result.setSuccess(false);
                result.setErrorType(IngestTaskErrorType.OCR_REQUEST_FAILED.name());
                result.setErrorCode(IngestTaskErrorType.OCR_REQUEST_FAILED.getCode());
                result.setText("");
                result.setTextLength(0);
                result.setErrorMessage("HTTP OCR 调用失败，响应状态异常");
                return result;
            }

            JsonNode root = objectMapper.readTree(response.getBody().getBytes(StandardCharsets.UTF_8));
            JsonNode textNode = readPath(root, responseField);
            JsonNode successNode = readPath(root, responseSuccessField);
            JsonNode messageNode = readPath(root, responseMessageField);
            JsonNode codeNode = readPath(root, responseCodeField);
            if (textNode.isMissingNode() || textNode.isNull()) {
                log.warn("HTTP OCR 响应中未找到字段 {}, fileName: {}", responseField, fileName);
                result.setSuccess(false);
                result.setErrorType(IngestTaskErrorType.OCR_RESPONSE_INVALID.name());
                result.setErrorCode(IngestTaskErrorType.OCR_RESPONSE_INVALID.getCode());
                result.setText("");
                result.setTextLength(0);
                result.setErrorMessage(resolveErrorMessage(messageNode, codeNode, "HTTP OCR 响应中未找到文本字段: " + responseField));
                return result;
            }
            String extractedText = textNode.asText("");
            result.setText(extractedText);
            result.setTextLength(extractedText == null ? 0 : extractedText.length());
            boolean successByField = isSuccess(successNode, codeNode);
            boolean successByText = extractedText != null && !extractedText.trim().isEmpty();
            result.setSuccess(successByField && successByText);
            if (!result.isSuccess()) {
                result.setErrorType(IngestTaskErrorType.OCR_RESPONSE_INVALID.name());
                result.setErrorCode(IngestTaskErrorType.OCR_RESPONSE_INVALID.getCode());
                result.setErrorMessage(resolveErrorMessage(messageNode, codeNode, "OCR 返回空文本"));
            }
            return result;
        } catch (Exception e) {
            log.error("HTTP OCR 调用失败，fileName: {}", fileName, e);
            result.setDurationMs(System.currentTimeMillis() - startTime);
            result.setSuccess(false);
            result.setErrorType(IngestTaskErrorType.OCR_REQUEST_FAILED.name());
            result.setErrorCode(IngestTaskErrorType.OCR_REQUEST_FAILED.getCode());
            result.setText("");
            result.setTextLength(0);
            result.setErrorMessage("HTTP OCR 调用异常: " + e.getMessage());
            return result;
        }
    }

    private String buildAuthHeaderValue() {
        String prefix = authHeaderPrefix == null ? "" : authHeaderPrefix;
        return prefix + ocrApiKey.trim();
    }

    private String resolveRequestFileField() {
        return requestFileField == null || requestFileField.trim().isEmpty() ? "fileBase64" : requestFileField.trim();
    }

    private boolean isSuccess(JsonNode successNode, JsonNode codeNode) {
        Set<String> successValues = parseSuccessValues();
        if (!successNode.isMissingNode() && !successNode.isNull()) {
            String successText = nodeToText(successNode);
            if (!successText.isEmpty()) {
                return successValues.contains(successText.toLowerCase());
            }
        }
        if (!codeNode.isMissingNode() && !codeNode.isNull()) {
            String codeText = nodeToText(codeNode);
            if (!codeText.isEmpty()) {
                return successValues.contains(codeText.toLowerCase());
            }
        }
        return true;
    }

    private String resolveErrorMessage(JsonNode messageNode, JsonNode codeNode, String fallback) {
        String message = nodeToText(messageNode);
        String code = nodeToText(codeNode);
        if (!message.isEmpty() && !code.isEmpty()) {
            return code + ": " + message;
        }
        if (!message.isEmpty()) {
            return message;
        }
        if (!code.isEmpty()) {
            return code;
        }
        return fallback;
    }

    private Set<String> parseSuccessValues() {
        if (responseSuccessValues == null || responseSuccessValues.trim().isEmpty()) {
            return Collections.singleton("true");
        }
        return java.util.Arrays.stream(responseSuccessValues.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private JsonNode readPath(JsonNode root, String path) {
        if (root == null || path == null || path.trim().isEmpty()) {
            return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
        }
        JsonNode current = root;
        for (String part : path.trim().split("\\.")) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
            }
            current = current.path(part);
        }
        return current == null ? com.fasterxml.jackson.databind.node.MissingNode.getInstance() : current;
    }

    private String nodeToText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.isValueNode() ? node.asText("") : node.toString();
    }

    private String preview(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 300) {
            return normalized;
        }
        return normalized.substring(0, 300) + "...";
    }
}
