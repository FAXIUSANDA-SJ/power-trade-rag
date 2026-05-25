package com.powertrade.core.service.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powertrade.core.model.ConfigVersionSummary;
import com.powertrade.dal.entity.ConfigVersionEntity;
import com.powertrade.dal.mapper.ConfigVersionMapper;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigVersionService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ConfigVersionMapper configVersionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> T getActiveConfig(String configType, String configKey, Class<T> clazz) {
        ConfigVersionEntity entity = configVersionMapper.selectOne(
                new LambdaQueryWrapper<ConfigVersionEntity>()
                        .eq(ConfigVersionEntity::getConfigType, configType)
                        .eq(ConfigVersionEntity::getConfigKey, configKey)
                        .eq(ConfigVersionEntity::getIsActive, 1)
                        .orderByDesc(ConfigVersionEntity::getVersionNo)
                        .last("limit 1")
        );
        if (entity == null) {
            return null;
        }
        return deserialize(entity.getConfigPayload(), clazz);
    }

    public <T> ConfigVersionEntity saveAsNewActiveVersion(
            String configType,
            String configKey,
            T payload,
            String description,
            String operator) {
        Integer latestVersion = getLatestVersionNo(configType, configKey);

        configVersionMapper.update(
                null,
                new LambdaUpdateWrapper<ConfigVersionEntity>()
                        .eq(ConfigVersionEntity::getConfigType, configType)
                        .eq(ConfigVersionEntity::getConfigKey, configKey)
                        .set(ConfigVersionEntity::getIsActive, 0)
        );

        ConfigVersionEntity entity = new ConfigVersionEntity();
        entity.setConfigType(configType);
        entity.setConfigKey(configKey);
        entity.setVersionNo(latestVersion + 1);
        entity.setIsActive(1);
        entity.setDescription(description);
        entity.setConfigPayload(serialize(payload));
        entity.setCreator(operator);
        entity.setUpdater(operator);
        configVersionMapper.insert(entity);
        return entity;
    }

    public Integer getLatestVersionNo(String configType, String configKey) {
        ConfigVersionEntity entity = configVersionMapper.selectOne(
                new LambdaQueryWrapper<ConfigVersionEntity>()
                        .eq(ConfigVersionEntity::getConfigType, configType)
                        .eq(ConfigVersionEntity::getConfigKey, configKey)
                        .orderByDesc(ConfigVersionEntity::getVersionNo)
                        .last("limit 1")
        );
        return entity == null || entity.getVersionNo() == null ? 0 : entity.getVersionNo();
    }

    public List<ConfigVersionSummary> listVersions(String configType, String configKey) {
        return configVersionMapper.selectList(
                new LambdaQueryWrapper<ConfigVersionEntity>()
                        .eq(ConfigVersionEntity::getConfigType, configType)
                        .eq(ConfigVersionEntity::getConfigKey, configKey)
                        .orderByDesc(ConfigVersionEntity::getVersionNo)
        ).stream().map(this::toSummary).collect(Collectors.toList());
    }

    public <T> T activateVersion(String configType, String configKey, Integer versionNo, Class<T> clazz) {
        ConfigVersionEntity target = configVersionMapper.selectOne(
                new LambdaQueryWrapper<ConfigVersionEntity>()
                        .eq(ConfigVersionEntity::getConfigType, configType)
                        .eq(ConfigVersionEntity::getConfigKey, configKey)
                        .eq(ConfigVersionEntity::getVersionNo, versionNo)
                        .last("limit 1")
        );
        if (target == null) {
            return null;
        }

        configVersionMapper.update(
                null,
                new LambdaUpdateWrapper<ConfigVersionEntity>()
                        .eq(ConfigVersionEntity::getConfigType, configType)
                        .eq(ConfigVersionEntity::getConfigKey, configKey)
                        .set(ConfigVersionEntity::getIsActive, 0)
        );

        target.setIsActive(1);
        configVersionMapper.updateById(target);
        return deserialize(target.getConfigPayload(), clazz);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("配置序列化失败", e);
        }
    }

    private <T> T deserialize(String payload, Class<T> clazz) {
        try {
            return objectMapper.readValue(payload, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("配置反序列化失败", e);
        }
    }

    private ConfigVersionSummary toSummary(ConfigVersionEntity entity) {
        ConfigVersionSummary summary = new ConfigVersionSummary();
        summary.setId(entity.getId());
        summary.setConfigType(entity.getConfigType());
        summary.setConfigKey(entity.getConfigKey());
        summary.setVersionNo(entity.getVersionNo());
        summary.setIsActive(entity.getIsActive());
        summary.setDescription(entity.getDescription());
        summary.setCreator(entity.getCreator());
        summary.setCreateTime(entity.getCreateTime() == null ? null : TIME_FORMATTER.format(entity.getCreateTime().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()));
        return summary;
    }
}
