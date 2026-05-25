package com.powertrade.core.service.config;

import com.powertrade.core.model.ConfigVersionSummary;
import com.powertrade.core.model.RetrievalConfig;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RetrievalConfigService {

    private static final String CONFIG_TYPE = "retrieval";
    private static final String CONFIG_KEY = "system";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConfigVersionService configVersionService;

    public RetrievalConfigService(ConfigVersionService configVersionService) {
        this.configVersionService = configVersionService;
    }

    public RetrievalConfig getActiveConfig() {
        RetrievalConfig config = configVersionService.getActiveConfig(CONFIG_TYPE, CONFIG_KEY, RetrievalConfig.class);
        return config == null ? buildDefaultConfig() : sanitize(config);
    }

    public RetrievalConfig saveConfig(RetrievalConfig request) {
        RetrievalConfig config = sanitize(request == null ? buildDefaultConfig() : request);
        Integer latestVersion = configVersionService.getLatestVersionNo(CONFIG_TYPE, CONFIG_KEY);
        config.setConfigKey(CONFIG_KEY);
        config.setVersionNo(latestVersion + 1);
        config.setUpdatedAt(LocalDateTime.now().format(TIME_FORMATTER));
        configVersionService.saveAsNewActiveVersion(CONFIG_TYPE, CONFIG_KEY, config, "检索参数配置变更", "system");
        return config;
    }

    public List<ConfigVersionSummary> listVersions() {
        return configVersionService.listVersions(CONFIG_TYPE, CONFIG_KEY);
    }

    public RetrievalConfig activateVersion(Integer versionNo) {
        RetrievalConfig config = configVersionService.activateVersion(CONFIG_TYPE, CONFIG_KEY, versionNo, RetrievalConfig.class);
        if (config == null) {
            throw new RuntimeException("指定版本不存在: " + versionNo);
        }
        return sanitize(config);
    }

    private RetrievalConfig buildDefaultConfig() {
        RetrievalConfig config = new RetrievalConfig();
        config.setConfigKey(CONFIG_KEY);
        config.setVersionNo(1);
        config.setTopK(5);
        config.setMinScore(0.6D);
        config.setSearchMode("vector");
        config.setRerankEnabled(Boolean.FALSE);
        config.setMaxReferenceCount(5);
        config.setUpdatedAt(LocalDateTime.now().format(TIME_FORMATTER));
        return config;
    }

    private RetrievalConfig sanitize(RetrievalConfig config) {
        RetrievalConfig sanitized = new RetrievalConfig();
        sanitized.setConfigKey(config.getConfigKey() == null ? CONFIG_KEY : config.getConfigKey());
        sanitized.setVersionNo(config.getVersionNo());
        sanitized.setTopK(config.getTopK() == null || config.getTopK() < 1 ? 5 : config.getTopK());
        sanitized.setMinScore(config.getMinScore() == null ? 0.6D : config.getMinScore());
        sanitized.setSearchMode(config.getSearchMode() == null || config.getSearchMode().trim().isEmpty() ? "vector" : config.getSearchMode().trim());
        sanitized.setRerankEnabled(config.getRerankEnabled() == null ? Boolean.FALSE : config.getRerankEnabled());
        sanitized.setMaxReferenceCount(config.getMaxReferenceCount() == null || config.getMaxReferenceCount() < 1 ? 5 : config.getMaxReferenceCount());
        sanitized.setUpdatedAt(config.getUpdatedAt() == null ? LocalDateTime.now().format(TIME_FORMATTER) : config.getUpdatedAt());
        return sanitized;
    }
}
