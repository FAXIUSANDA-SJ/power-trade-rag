package com.powertrade.core.service.config;

import com.powertrade.core.model.ConfigVersionSummary;
import com.powertrade.core.model.KnowledgeBaseConfig;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseConfigService {

    private static final String CONFIG_TYPE = "knowledge_base";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConfigVersionService configVersionService;

    public KnowledgeBaseConfigService(ConfigVersionService configVersionService) {
        this.configVersionService = configVersionService;
    }

    public KnowledgeBaseConfig getActiveConfig(String kbId) {
        KnowledgeBaseConfig config = configVersionService.getActiveConfig(CONFIG_TYPE, kbId, KnowledgeBaseConfig.class);
        if (config == null) {
            return buildDefaultConfig(kbId);
        }
        return sanitize(kbId, config);
    }

    public KnowledgeBaseConfig saveConfig(String kbId, KnowledgeBaseConfig request) {
        KnowledgeBaseConfig config = sanitize(kbId, request == null ? buildDefaultConfig(kbId) : request);
        Integer latestVersion = configVersionService.getLatestVersionNo(CONFIG_TYPE, kbId);
        config.setVersionNo(latestVersion + 1);
        config.setUpdatedAt(LocalDateTime.now().format(TIME_FORMATTER));
        configVersionService.saveAsNewActiveVersion(CONFIG_TYPE, kbId, config, "知识库配置变更", "system");
        return config;
    }

    public List<ConfigVersionSummary> listVersions(String kbId) {
        return configVersionService.listVersions(CONFIG_TYPE, kbId);
    }

    public KnowledgeBaseConfig activateVersion(String kbId, Integer versionNo) {
        KnowledgeBaseConfig config = configVersionService.activateVersion(CONFIG_TYPE, kbId, versionNo, KnowledgeBaseConfig.class);
        if (config == null) {
            throw new RuntimeException("指定版本不存在: " + versionNo);
        }
        return sanitize(kbId, config);
    }

    private KnowledgeBaseConfig buildDefaultConfig(String kbId) {
        KnowledgeBaseConfig config = new KnowledgeBaseConfig();
        config.setKbId(kbId);
        config.setVersionNo(1);
        config.setVectorModel("text-embedding-ada-002");
        config.setParseStrategy("tika");
        config.setChunkSize(300);
        config.setChunkOverlap(30);
        config.setOcrEnabled(Boolean.FALSE);
        config.setUpdatedAt(LocalDateTime.now().format(TIME_FORMATTER));
        return config;
    }

    private KnowledgeBaseConfig sanitize(String kbId, KnowledgeBaseConfig config) {
        KnowledgeBaseConfig sanitized = new KnowledgeBaseConfig();
        sanitized.setKbId(kbId);
        sanitized.setVersionNo(config.getVersionNo());
        sanitized.setVectorModel(config.getVectorModel() == null || config.getVectorModel().trim().isEmpty()
                ? "text-embedding-ada-002" : config.getVectorModel().trim());
        sanitized.setParseStrategy(config.getParseStrategy() == null || config.getParseStrategy().trim().isEmpty()
                ? "tika" : config.getParseStrategy().trim());
        sanitized.setChunkSize(config.getChunkSize() == null || config.getChunkSize() < 1 ? 300 : config.getChunkSize());
        sanitized.setChunkOverlap(config.getChunkOverlap() == null || config.getChunkOverlap() < 0 ? 30 : config.getChunkOverlap());
        sanitized.setOcrEnabled(config.getOcrEnabled() == null ? Boolean.FALSE : config.getOcrEnabled());
        sanitized.setUpdatedAt(config.getUpdatedAt() == null ? LocalDateTime.now().format(TIME_FORMATTER) : config.getUpdatedAt());
        return sanitized;
    }
}
