package com.powertrade.api.controller;

import com.powertrade.core.model.IngestTask;
import com.powertrade.core.model.PageResult;
import com.powertrade.core.service.IngestTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingest-task")
@Api(tags = "摄取任务接口")
public class IngestTaskController {

    private final IngestTaskService ingestTaskService;

    public IngestTaskController(IngestTaskService ingestTaskService) {
        this.ingestTaskService = ingestTaskService;
    }

    @GetMapping("/list")
    @ApiOperation("获取摄取任务列表")
    public PageResult<IngestTask> listTasks(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "kbId", required = false) String kbId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return ingestTaskService.listTasks(status, kbId, page, size);
    }

    @GetMapping("/{taskId}")
    @ApiOperation("获取摄取任务详情")
    public IngestTask getTask(@PathVariable String taskId) {
        return ingestTaskService.getTask(taskId);
    }

    @PostMapping("/{taskId}/retry")
    @ApiOperation("重试摄取任务")
    public IngestTask retryTask(@PathVariable String taskId) {
        return ingestTaskService.retryTask(taskId);
    }

    @GetMapping("/stats")
    @ApiOperation("获取摄取任务状态统计")
    public Map<String, Long> getTaskStats(
            @RequestParam(value = "kbId", required = false) String kbId) {
        return ingestTaskService.getTaskStatusStats(kbId);
    }
}
