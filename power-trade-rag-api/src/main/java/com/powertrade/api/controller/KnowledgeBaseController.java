package com.powertrade.api.controller;

import com.powertrade.core.model.KnowledgeBase;
import com.powertrade.core.service.KnowledgeBaseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理控制器
 */
@RestController
@RequestMapping("/api/knowledge")
@Api(tags = "知识库管理接口")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @PostMapping("/create")
    @ApiOperation("创建知识库")
    public KnowledgeBase createKnowledgeBase(@RequestBody KnowledgeBase kb) {
        return knowledgeBaseService.createKnowledgeBase(kb);
    }

    @GetMapping("/list")
    @ApiOperation("获取知识库列表")
    public List<KnowledgeBase> getKnowledgeBaseList() {
        return knowledgeBaseService.getKnowledgeBaseList();
    }

    @DeleteMapping("/{kbId}")
    @ApiOperation("删除知识库")
    public boolean deleteKnowledgeBase(@PathVariable String kbId) {
        return knowledgeBaseService.deleteKnowledgeBase(kbId);
    }
}
