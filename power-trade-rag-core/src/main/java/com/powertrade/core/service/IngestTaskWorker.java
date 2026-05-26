package com.powertrade.core.service;

import com.powertrade.common.exception.IngestTaskErrorType;
import com.powertrade.common.exception.IngestTaskException;
import com.powertrade.core.model.DocumentInfo;
import com.powertrade.core.model.IngestTask;
import com.powertrade.core.service.rag.RagCoreService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IngestTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(IngestTaskWorker.class);

    private final IngestTaskService ingestTaskService;
    private final DocumentService documentService;
    private final RagCoreService ragCoreService;

    @Value("${rag.ingest.worker-batch-size:5}")
    private int workerBatchSize;

    public IngestTaskWorker(
            IngestTaskService ingestTaskService,
            DocumentService documentService,
            RagCoreService ragCoreService) {
        this.ingestTaskService = ingestTaskService;
        this.documentService = documentService;
        this.ragCoreService = ragCoreService;
    }

    @Scheduled(fixedDelayString = "${rag.ingest.worker-delay-ms:5000}")
    public void consumePendingTasks() {
        List<IngestTask> tasks = ingestTaskService.pickExecutableTasks(Math.max(workerBatchSize, 1));
        if (!tasks.isEmpty()) {
            log.debug("本轮准备执行摄取任务数量: {}", tasks.size());
        }
        for (IngestTask task : tasks) {
            processTask(task);
        }
    }

    private void processTask(IngestTask task) {
        try {
            if (!ingestTaskService.tryMarkRunning(task.getTaskId())) {
                log.debug("任务已被其他工作线程抢占，跳过: {}", task.getTaskId());
                return;
            }
            DocumentInfo documentInfo = documentService.getDocumentByDocId(task.getDocId());
            if (documentInfo == null) {
                throw new IngestTaskException(IngestTaskErrorType.DOC_NOT_FOUND, "文档不存在: " + task.getDocId());
            }

            if (ragCoreService.hasProcessedDocument(task.getDocId())) {
                log.info("文档已存在向量结果，直接标记成功，docId: {}", task.getDocId());
                ingestTaskService.markSuccess(task.getTaskId());
                return;
            }

            String content = "TEXT".equals(documentInfo.getDocType())
                    ? documentService.getStoredContent(task.getDocId())
                    : documentService.readDocumentContent(documentInfo.getFilePath());
            ragCoreService.processAndStoreDocumentContent(content, task.getKbId(), task.getDocId(), documentInfo.getFileName());
            ingestTaskService.markSuccess(task.getTaskId());
        } catch (Exception e) {
            log.error("摄取任务执行失败，taskId: {}", task.getTaskId(), e);
            ingestTaskService.markFailed(task.getTaskId(), e);
        }
    }
}
