package com.powertrade.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.powertrade.core.model.IngestTask;
import com.powertrade.core.model.PageResult;
import com.powertrade.dal.entity.IngestTaskEntity;
import com.powertrade.dal.mapper.IngestTaskMapper;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class IngestTaskService {

    private final IngestTaskMapper ingestTaskMapper;

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
            throw new RuntimeException("任务正在执行中，不能重试: " + taskId);
        }
        if ("success".equalsIgnoreCase(entity.getStatus())) {
            throw new RuntimeException("任务已成功完成，不能重试: " + taskId);
        }
        entity.setStatus("pending");
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
                        .last("limit " + limit)
        );
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .filter(entity -> entity.getRetryCount() == null || entity.getRetryCount() < entity.getMaxRetryCount())
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
        IngestTask task = new IngestTask();
        task.setTaskId(entity.getTaskId());
        task.setDocId(entity.getDocId());
        task.setKbId(entity.getKbId());
        task.setTaskType(entity.getTaskType());
        task.setStatus(entity.getStatus());
        task.setRetryCount(entity.getRetryCount());
        task.setMaxRetryCount(entity.getMaxRetryCount());
        task.setErrorMessage(entity.getErrorMessage());
        task.setCreateTime(entity.getCreateTime());
        task.setUpdateTime(entity.getUpdateTime());
        return task;
    }
}
