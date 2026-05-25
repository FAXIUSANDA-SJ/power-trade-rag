package com.powertrade.api.controller;

import com.powertrade.core.model.ConfigVersionSummary;
import com.powertrade.core.model.PromptConfig;
import java.util.List;
import com.powertrade.core.service.rag.PromptConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 提示词配置控制器
 */
@RestController
@RequestMapping("/api/prompt-config")
@Api(tags = "提示词配置接口")
public class PromptConfigController {

    @Autowired
    private PromptConfigService promptConfigService;

    @GetMapping
    @ApiOperation("获取提示词配置")
    public PromptConfig getConfig() {
        return promptConfigService.getConfig();
    }

    @PutMapping
    @ApiOperation("更新提示词配置")
    public PromptConfig updateConfig(@RequestBody PromptConfig request) {
        return promptConfigService.updateConfig(request);
    }

    @PostMapping("/reset")
    @ApiOperation("重置默认提示词配置")
    public PromptConfig resetDefault() {
        return promptConfigService.resetDefault();
    }

    @GetMapping("/versions")
    @ApiOperation("获取提示词配置版本列表")
    public List<ConfigVersionSummary> listVersions() {
        return promptConfigService.listVersions();
    }

    @PostMapping("/versions/{versionNo}/activate")
    @ApiOperation("切换指定提示词配置版本")
    public PromptConfig activateVersion(@PathVariable Integer versionNo) {
        return promptConfigService.activateVersion(versionNo);
    }
}
