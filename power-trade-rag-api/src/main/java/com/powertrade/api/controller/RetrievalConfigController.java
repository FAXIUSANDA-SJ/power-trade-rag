package com.powertrade.api.controller;

import com.powertrade.core.model.ConfigVersionSummary;
import com.powertrade.core.model.RetrievalConfig;
import com.powertrade.core.service.config.RetrievalConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/retrieval-config")
@Api(tags = "检索参数配置接口")
public class RetrievalConfigController {

    private final RetrievalConfigService retrievalConfigService;

    public RetrievalConfigController(RetrievalConfigService retrievalConfigService) {
        this.retrievalConfigService = retrievalConfigService;
    }

    @GetMapping
    @ApiOperation("获取当前检索参数配置")
    public RetrievalConfig getConfig() {
        return retrievalConfigService.getActiveConfig();
    }

    @PutMapping
    @ApiOperation("更新检索参数配置")
    public RetrievalConfig updateConfig(@RequestBody RetrievalConfig request) {
        return retrievalConfigService.saveConfig(request);
    }

    @GetMapping("/versions")
    @ApiOperation("获取检索参数配置版本列表")
    public List<ConfigVersionSummary> listVersions() {
        return retrievalConfigService.listVersions();
    }

    @PostMapping("/versions/{versionNo}/activate")
    @ApiOperation("切换指定检索参数配置版本")
    public RetrievalConfig activateVersion(@PathVariable Integer versionNo) {
        return retrievalConfigService.activateVersion(versionNo);
    }
}
