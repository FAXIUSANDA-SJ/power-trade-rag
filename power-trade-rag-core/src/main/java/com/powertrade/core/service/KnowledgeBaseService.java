package com.powertrade.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.powertrade.core.model.KnowledgeBase;
import com.powertrade.core.model.KnowledgeBaseConfig;
import com.powertrade.core.service.config.KnowledgeBaseConfigService;
import com.powertrade.dal.entity.KnowledgeBaseEntity;
import com.powertrade.dal.mapper.KnowledgeBaseMapper;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseConfigService knowledgeBaseConfigService;

    public KnowledgeBaseService(
            KnowledgeBaseMapper knowledgeBaseMapper,
            KnowledgeBaseConfigService knowledgeBaseConfigService) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeBaseConfigService = knowledgeBaseConfigService;
    }

    public KnowledgeBase createKnowledgeBase(KnowledgeBase request) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setKbId(request.getKbId() == null || request.getKbId().trim().isEmpty()
                ? "KB-" + UUID.randomUUID().toString()
                : request.getKbId());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreator("system");
        knowledgeBaseMapper.insert(entity);
        knowledgeBaseConfigService.saveConfig(entity.getKbId(), null);
        return toModel(entity);
    }

    public List<KnowledgeBase> getKnowledgeBaseList() {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .eq(KnowledgeBaseEntity::getStatus, 1)
                        .orderByDesc(KnowledgeBaseEntity::getCreateTime)
        ).stream().map(this::toModel).collect(Collectors.toList());
    }

    public boolean deleteKnowledgeBase(String kbId) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBaseEntity>().eq(KnowledgeBaseEntity::getKbId, kbId)
        );
        if (entity == null) {
            return false;
        }
        entity.setStatus(0);
        return knowledgeBaseMapper.updateById(entity) > 0;
    }

    private KnowledgeBase toModel(KnowledgeBaseEntity entity) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setKbId(entity.getKbId());
        knowledgeBase.setName(entity.getName());
        knowledgeBase.setDescription(entity.getDescription());
        knowledgeBase.setStatus(entity.getStatus());
        knowledgeBase.setCreateTime(entity.getCreateTime());
        KnowledgeBaseConfig config = knowledgeBaseConfigService.getActiveConfig(entity.getKbId());
        knowledgeBase.setConfigVersionNo(config.getVersionNo());
        knowledgeBase.setVectorModel(config.getVectorModel());
        knowledgeBase.setParseStrategy(config.getParseStrategy());
        knowledgeBase.setChunkSize(config.getChunkSize());
        knowledgeBase.setChunkOverlap(config.getChunkOverlap());
        knowledgeBase.setOcrEnabled(config.getOcrEnabled());
        return knowledgeBase;
    }
}
