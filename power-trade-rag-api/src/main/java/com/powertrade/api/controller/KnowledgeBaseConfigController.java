package com.powertrade.api.controller;

import com.powertrade.core.model.ConfigVersionSummary;
import com.powertrade.core.model.KnowledgeBaseConfig;
import com.powertrade.core.service.config.KnowledgeBaseConfigService;
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
@RequestMapping("/api/knowledge-config")
@Api(tags = "知识库配置接口")
public class KnowledgeBaseConfigController {

    private final KnowledgeBaseConfigService knowledgeBaseConfigService;

    public KnowledgeBaseConfigController(KnowledgeBaseConfigService knowledgeBaseConfigService) {
        this.knowledgeBaseConfigService = knowledgeBaseConfigService;
    }

    @GetMapping("/{kbId}")
    @ApiOperation("获取当前知识库配置")
    public KnowledgeBaseConfig getConfig(@PathVariable String kbId) {
        return knowledgeBaseConfigService.getActiveConfig(kbId);
    }

    @PutMapping("/{kbId}")
    @ApiOperation("更新知识库配置")
    public KnowledgeBaseConfig updateConfig(@PathVariable String kbId, @RequestBody KnowledgeBaseConfig request) {
        return knowledgeBaseConfigService.saveConfig(kbId, request);
    }

    @GetMapping("/{kbId}/versions")
    @ApiOperation("获取知识库配置版本列表")
    public List<ConfigVersionSummary> listVersions(@PathVariable String kbId) {
        return knowledgeBaseConfigService.listVersions(kbId);
    }

    @PostMapping("/{kbId}/versions/{versionNo}/activate")
    @ApiOperation("切换指定知识库配置版本")
    public KnowledgeBaseConfig activateVersion(@PathVariable String kbId, @PathVariable Integer versionNo) {
        return knowledgeBaseConfigService.activateVersion(kbId, versionNo);
    }
}
