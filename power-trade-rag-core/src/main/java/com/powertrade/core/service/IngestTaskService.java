package com.powertrade.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.powertrade.common.exception.IngestTaskErrorType;
import com.powertrade.common.exception.IngestTaskException;
import com.powertrade.core.model.IngestTask;
import com.powertrade.core.model.IngestTaskErrorDetail;
import com.powertrade.core.model.PageResult;
import com.powertrade.dal.entity.IngestTaskEntity;
import com.powertrade.dal.mapper.IngestTaskMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IngestTaskService {

    private static final String STRUCTURED_ERROR_PREFIX = "TASK_ERROR|";

    private final IngestTaskMapper ingestTaskMapper;

    @Value("${rag.ingest.retry-enabled:true}")
    private boolean retryEnabled;

    @Value("${rag.ingest.retry-base-delay-ms:10000}")
    private long retryBaseDelayMs;

    @Value("${rag.ingest.retry-max-delay-ms:300000}")
    private long retryMaxDelayMs;

    public IngestTaskService(IngestTaskMapper ingestTaskMapper) {
        this.ingestTaskMapper = ingestTaskMapper;
    }

    public IngestTask createPendingTask(String docId, String kbId, String taskType) {
        IngestTaskEntity entity = new IngestTaskEntity();
        entity.setTaskId("TASK-" + UUID.randomUUID().toString());
        entity.setDocId(docId);
        entity.setKbId(kbId);
        entity.setTaskType(taskType);
        entity.setStatus("pending");
        entity.setRetryCount(0);
        entity.setMaxRetryCount(3);
        entity.setCreator("system");
        ingestTaskMapper.insert(entity);
        return toModel(entity);
    }

    public void markRunning(String taskId) {
        updateStatus(taskId, "running", null);
    }

    public boolean tryMarkRunning(String taskId) {
        return ingestTaskMapper.update(
                null,
                new LambdaUpdateWrapper<IngestTaskEntity>()
                        .eq(IngestTaskEntity::getTaskId, taskId)
                        .in(IngestTaskEntity::getStatus, "pending", "failed")
                        .set(IngestTaskEntity::getStatus, "running")
                        .set(IngestTaskEntity::getErrorMessage, null)
                        .set(IngestTaskEntity::getUpdater, "system")
        ) > 0;
    }

    public void markSuccess(String taskId) {
        updateStatus(taskId, "success", null);
    }

    public void markFailed(String taskId, Throwable error) {
        markFailed(taskId, buildStoredErrorMessage(error));
    }

    public void markFailed(String taskId, String errorMessage) {
        IngestTaskEntity entity = findEntity(taskId);
        if (entity == null) {
            return;
        }
        entity.setStatus("failed");
        entity.setRetryCount((entity.getRetryCount() == null ? 0 : entity.getRetryCount()) + 1);
        entity.setErrorMessage(errorMessage);
        entity.setUpdater("system");
        ingestTaskMapper.updateById(entity);
    }

    public List<IngestTask> listPendingTasks() {
        return ingestTaskMapper.selectList(
                new LambdaQueryWrapper<IngestTaskEntity>()
                        .in(IngestTaskEntity::getStatus, "pending", "failed")
                        .orderByAsc(IngestTaskEntity::getCreateTime)
        ).stream().map(this::toModel).collect(Collectors.toList());
    }

    public PageResult<IngestTask> listTasks(String status, String kbId, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;

        LambdaQueryWrapper<IngestTaskEntity> countQuery = new LambdaQueryWrapper<IngestTaskEntity>()
                .eq(kbId != null && !kbId.trim().isEmpty(), IngestTaskEntity::getKbId, kbId)
                .eq(status != null && !status.trim().isEmpty(), IngestTaskEntity::getStatus, status);
        Long total = ingestTaskMapper.selectCount(countQuery);

        LambdaQueryWrapper<IngestTaskEntity> listQuery = new LambdaQueryWrapper<IngestTaskEntity>()
                .eq(kbId != null && !kbId.trim().isEmpty(), IngestTaskEntity::getKbId, kbId)
                .eq(status != null && !status.trim().isEmpty(), IngestTaskEntity::getStatus, status)
                .orderByDesc(IngestTaskEntity::getCreateTime)
                .last("limit " + offset + "," + safeSize);

        PageResult<IngestTask> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(safePage);
        result.setSize(safeSize);
        result.setRecords(ingestTaskMapper.selectList(listQuery).stream().map(this::toModel).collect(Collectors.toList()));
        return result;
    }

    public IngestTask getTask(String taskId) {
        IngestTaskEntity entity = findEntity(taskId);
        return entity == null ? null : toModel(entity);
    }

    public IngestTask getLatestTaskByDocId(String docId) {
        IngestTaskEntity entity = ingestTaskMapper.selectOne(
                new LambdaQueryWrapper<IngestTaskEntity>()
                        .eq(IngestTaskEntity::getDocId, docId)
                        .orderByDesc(IngestTaskEntity::getCreateTime)
                        .last("limit 1")
        );
        return entity == null ? null : toModel(entity);
    }

    public java.util.Map<String, Long> getTaskStatusStats(String kbId) {
        java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
        stats.put("pending", countByStatus("pending", kbId));
        stats.put("running", countByStatus("running", kbId));
        stats.put("success", countByStatus("success", kbId));
        stats.put("failed", countByStatus("failed", kbId));
        stats.put("total", stats.values().stream().mapToLong(Long::longValue).sum());
        return stats;
    }

    public IngestTask retryTask(String taskId) {
        IngestTaskEntity entity = findEntity(taskId);
        if (entity == null) {
            return null;
        }
        if ("running".equalsIgnoreCase(entity.getStatus())) {
            throw new IngestTaskException(IngestTaskErrorType.RETRY_NOT_ALLOWED, "任务正在执行中，不能重试: " + taskId);
        }
        if ("success".equalsIgnoreCase(entity.getStatus())) {
            throw new IngestTaskException(IngestTaskErrorType.RETRY_NOT_ALLOWED, "任务已成功完成，不能重试: " + taskId);
        }
        entity.setStatus("pending");
        entity.setRetryCount(0);
        entity.setErrorMessage(null);
        entity.setUpdater("system");
        ingestTaskMapper.updateById(entity);
        return toModel(entity);
    }

    public List<IngestTask> pickExecutableTasks(int limit) {
        List<IngestTaskEntity> entities = ingestTaskMapper.selectList(
                new LambdaQueryWrapper<IngestTaskEntity>()
                        .in(IngestTaskEntity::getStatus, "pending", "failed")
                        .orderByAsc(IngestTaskEntity::getCreateTime)
                        .last("limit " + Math.max(limit * 5, 20))
        );
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .filter(this::canAutoRetry)
                .filter(this::isReadyForExecution)
                .limit(limit)
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    private void updateStatus(String taskId, String status, String errorMessage) {
        IngestTaskEntity entity = findEntity(taskId);
        if (entity == null) {
            return;
        }
        entity.setStatus(status);
        entity.setErrorMessage(errorMessage);
        entity.setUpdater("system");
        ingestTaskMapper.updateById(entity);
    }

    private IngestTaskEntity findEntity(String taskId) {
        return ingestTaskMapper.selectOne(
                new LambdaQueryWrapper<IngestTaskEntity>()
                        .eq(IngestTaskEntity::getTaskId, taskId)
                        .last("limit 1")
        );
    }

    private long countByStatus(String status, String kbId) {
        Long count = ingestTaskMapper.selectCount(
                new LambdaQueryWrapper<IngestTaskEntity>()
                        .eq(IngestTaskEntity::getStatus, status)
                        .eq(kbId != null && !kbId.trim().isEmpty(), IngestTaskEntity::getKbId, kbId)
        );
        return count == null ? 0L : count;
    }

    private IngestTask toModel(IngestTaskEntity entity) {
        ParsedTaskError parsedError = parseTaskError(entity.getErrorMessage());
        IngestTask task = new IngestTask();
        task.setTaskId(entity.getTaskId());
        task.setDocId(entity.getDocId());
        task.setKbId(entity.getKbId());
        task.setTaskType(entity.getTaskType());
        task.setStatus(entity.getStatus());
        task.setRetryCount(entity.getRetryCount());
        task.setMaxRetryCount(entity.getMaxRetryCount());
        task.setErrorType(parsedError.errorType.name());
        task.setErrorCode(parsedError.errorCode);
        task.setErrorMessage(parsedError.message);
        task.setErrorDetail(buildErrorDetail(entity));
        task.setRetryDelayMs(calculateRetryDelayMs(entity));
        task.setNextRetryTime(calculateNextRetryTime(entity));
        task.setRetryReady(isReadyForExecution(entity));
        task.setAutoRetryAllowed(isAutoRetryAllowed(entity));
        task.setRetryBlockedReason(resolveRetryBlockedReason(entity));
        task.setCreateTime(entity.getCreateTime());
        task.setUpdateTime(entity.getUpdateTime());
        return task;
    }

    private IngestTaskErrorDetail buildErrorDetail(IngestTaskEntity entity) {
        ParsedTaskError parsedError = parseTaskError(entity.getErrorMessage());
        if (parsedError.message == null || parsedError.message.trim().isEmpty()) {
            return null;
        }
        int retryCount = entity.getRetryCount() == null ? 0 : entity.getRetryCount();
        int maxRetryCount = entity.getMaxRetryCount() == null ? 0 : entity.getMaxRetryCount();
        IngestTaskErrorDetail detail = new IngestTaskErrorDetail();
        detail.setCategory(parsedError.errorType.getCategory());
        detail.setErrorType(parsedError.errorType.name());
        detail.setErrorCode(parsedError.errorCode);
        detail.setMessage(parsedError.message);
        detail.setSummary(summarize(parsedError.message));
        detail.setRetryCount(retryCount);
        detail.setMaxRetryCount(maxRetryCount);
        detail.setRetryExhausted(maxRetryCount > 0 && retryCount >= maxRetryCount);
        detail.setCanRetry(!"running".equalsIgnoreCase(entity.getStatus())
                && !"success".equalsIgnoreCase(entity.getStatus()));
        detail.setAutoRetryAllowed(isAutoRetryAllowed(entity));
        detail.setRetryReady(isReadyForExecution(entity));
        detail.setRetryDelayMs(calculateRetryDelayMs(entity));
        detail.setRetryBlockedReason(resolveRetryBlockedReason(entity));
        detail.setNextRetryTime(calculateNextRetryTime(entity));
        detail.setLastOccurredAt(entity.getUpdateTime());
        return detail;
    }

    private boolean canAutoRetry(IngestTaskEntity entity) {
        return isAutoRetryAllowed(entity);
    }

    private boolean isAutoRetryAllowed(IngestTaskEntity entity) {
        if (entity == null || entity.getStatus() == null) {
            return false;
        }
        if ("pending".equalsIgnoreCase(entity.getStatus())) {
            return true;
        }
        if (!"failed".equalsIgnoreCase(entity.getStatus()) || !retryEnabled) {
            return false;
        }
        if (entity.getRetryCount() != null && entity.getRetryCount() >= entity.getMaxRetryCount()) {
            return false;
        }
        return parseTaskError(entity.getErrorMessage()).autoRetryable;
    }

    private boolean isReadyForExecution(IngestTaskEntity entity) {
        if (entity == null || entity.getStatus() == null) {
            return false;
        }
        if ("pending".equalsIgnoreCase(entity.getStatus())) {
            return true;
        }
        if (!"failed".equalsIgnoreCase(entity.getStatus()) || !retryEnabled) {
            return false;
        }
        if (!isAutoRetryAllowed(entity)) {
            return false;
        }
        Date nextRetryTime = calculateNextRetryTime(entity);
        return nextRetryTime == null || !nextRetryTime.after(new Date());
    }

    private Date calculateNextRetryTime(IngestTaskEntity entity) {
        if (entity == null || !"failed".equalsIgnoreCase(entity.getStatus()) || !retryEnabled) {
            return null;
        }
        long retryDelayMs = calculateRetryDelayMs(entity);
        Date updateTime = entity.getUpdateTime();
        long baseTime = updateTime == null ? System.currentTimeMillis() : updateTime.getTime();
        return new Date(baseTime + retryDelayMs);
    }

    private Long calculateRetryDelayMs(IngestTaskEntity entity) {
        if (entity == null || !"failed".equalsIgnoreCase(entity.getStatus()) || !retryEnabled) {
            return 0L;
        }
        int retryCount = entity.getRetryCount() == null ? 0 : entity.getRetryCount();
        if (retryCount <= 0) {
            return retryBaseDelayMs;
        }
        long multiplier = 1L << Math.max(retryCount - 1, 0);
        long delay = retryBaseDelayMs * multiplier;
        if (retryMaxDelayMs > 0) {
            delay = Math.min(delay, retryMaxDelayMs);
        }
        return Math.max(delay, 0L);
    }

    private String resolveRetryBlockedReason(IngestTaskEntity entity) {
        if (entity == null || entity.getStatus() == null) {
            return "";
        }
        if ("pending".equalsIgnoreCase(entity.getStatus())) {
            return "";
        }
        if ("running".equalsIgnoreCase(entity.getStatus())) {
            return "任务执行中，等待当前执行完成";
        }
        if ("success".equalsIgnoreCase(entity.getStatus())) {
            return "任务已成功完成，无需自动重试";
        }
        if (!retryEnabled) {
            return "系统已关闭自动重试";
        }
        if (entity.getRetryCount() != null && entity.getRetryCount() >= entity.getMaxRetryCount()) {
            return "已达到最大自动重试次数";
        }
        ParsedTaskError parsedError = parseTaskError(entity.getErrorMessage());
        if (!parsedError.autoRetryable) {
            return "当前错误类型不允许自动重试: " + parsedError.errorCode;
        }
        if (!isReadyForExecution(entity)) {
            return "任务仍处于退避等待窗口";
        }
        return "";
    }

    private String buildStoredErrorMessage(Throwable error) {
        ParsedTaskError parsedError = parseThrowable(error);
        String encodedMessage = Base64.getEncoder().encodeToString(
                (parsedError.message == null ? "" : parsedError.message).getBytes(StandardCharsets.UTF_8)
        );
        return STRUCTURED_ERROR_PREFIX
                + parsedError.errorType.name()
                + "|"
                + parsedError.errorCode
                + "|"
                + (parsedError.autoRetryable ? "1" : "0")
                + "|"
                + encodedMessage;
    }

    private ParsedTaskError parseTaskError(String storedError) {
        if (storedError == null || storedError.trim().isEmpty()) {
            return ParsedTaskError.empty();
        }
        if (!storedError.startsWith(STRUCTURED_ERROR_PREFIX)) {
            return parseLegacyError(storedError);
        }
        String[] parts = storedError.split("\\|", 5);
        if (parts.length < 5) {
            return parseLegacyError(storedError);
        }
        IngestTaskErrorType errorType = parseErrorType(parts[1]);
        String errorCode = parts[2] == null || parts[2].trim().isEmpty() ? errorType.getCode() : parts[2].trim();
        boolean autoRetryable = "1".equals(parts[3]);
        String message = new String(Base64.getDecoder().decode(parts[4]), StandardCharsets.UTF_8);
        return new ParsedTaskError(errorType, errorCode, message, autoRetryable);
    }

    private ParsedTaskError parseLegacyError(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return ParsedTaskError.empty();
        }
        IngestTaskErrorType errorType = inferLegacyErrorType(errorMessage);
        return new ParsedTaskError(errorType, errorType.getCode(), errorMessage, errorType.isAutoRetryable());
    }

    private ParsedTaskError parseThrowable(Throwable error) {
        if (error instanceof IngestTaskException) {
            IngestTaskException ingestTaskException = (IngestTaskException) error;
            IngestTaskErrorType errorType = ingestTaskException.getErrorType() == null
                    ? IngestTaskErrorType.SYSTEM_ERROR
                    : ingestTaskException.getErrorType();
            return new ParsedTaskError(
                    errorType,
                    ingestTaskException.getErrorCode(),
                    safeMessage(ingestTaskException.getMessage(), errorType.getDescription()),
                    ingestTaskException.isAutoRetryable()
            );
        }
        String message = error == null ? "" : safeMessage(error.getMessage(), "系统异常");
        IngestTaskErrorType errorType = inferLegacyErrorType(message);
        return new ParsedTaskError(errorType, errorType.getCode(), message, errorType.isAutoRetryable());
    }

    private String safeMessage(String message, String fallback) {
        return message == null || message.trim().isEmpty() ? fallback : message;
    }

    private IngestTaskErrorType parseErrorType(String value) {
        try {
            return IngestTaskErrorType.valueOf(value);
        } catch (Exception ignored) {
            return IngestTaskErrorType.SYSTEM_ERROR;
        }
    }

    private IngestTaskErrorType inferLegacyErrorType(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return IngestTaskErrorType.SYSTEM_ERROR;
        }
        String normalized = errorMessage.toLowerCase();
        if (normalized.contains("文档不存在")) {
            return IngestTaskErrorType.DOC_NOT_FOUND;
        }
        if (normalized.contains("ocr")) {
            if (normalized.contains("endpoint 未配置")) {
                return IngestTaskErrorType.OCR_PROVIDER_NOT_CONFIGURED;
            }
            if (normalized.contains("provider 未启用")) {
                return IngestTaskErrorType.OCR_PROVIDER_DISABLED;
            }
            if (normalized.contains("文件过大")) {
                return IngestTaskErrorType.OCR_FILE_TOO_LARGE;
            }
            if (normalized.contains("未找到文本字段") || normalized.contains("返回空文本")) {
                return IngestTaskErrorType.OCR_RESPONSE_INVALID;
            }
            if (normalized.contains("读取文件失败")) {
                return IngestTaskErrorType.OCR_FILE_READ_FAILED;
            }
            return IngestTaskErrorType.OCR_REQUEST_FAILED;
        }
        if (normalized.contains("parse") || normalized.contains("解析")) {
            return IngestTaskErrorType.PARSE_FAILED;
        }
        if (normalized.contains("vector") || normalized.contains("embedding") || normalized.contains("向量")) {
            return IngestTaskErrorType.VECTOR_ERROR;
        }
        if (normalized.contains("storage") || normalized.contains("minio") || normalized.contains("文件")) {
            return IngestTaskErrorType.STORAGE_ERROR;
        }
        if (normalized.contains("配置") || normalized.contains("provider")) {
            return IngestTaskErrorType.CONFIGURATION_ERROR;
        }
        if (normalized.contains("不能重试")) {
            return IngestTaskErrorType.RETRY_NOT_ALLOWED;
        }
        return IngestTaskErrorType.SYSTEM_ERROR;
    }

    private String summarize(String errorMessage) {
        String normalized = errorMessage.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120) + "...";
    }

    private static class ParsedTaskError {
        private final IngestTaskErrorType errorType;
        private final String errorCode;
        private final String message;
        private final boolean autoRetryable;

        private ParsedTaskError(IngestTaskErrorType errorType, String errorCode, String message, boolean autoRetryable) {
            this.errorType = errorType;
            this.errorCode = errorCode;
            this.message = message;
            this.autoRetryable = autoRetryable;
        }

        private static ParsedTaskError empty() {
            return new ParsedTaskError(IngestTaskErrorType.SYSTEM_ERROR, IngestTaskErrorType.SYSTEM_ERROR.getCode(), "", false);
        }
    }
}
