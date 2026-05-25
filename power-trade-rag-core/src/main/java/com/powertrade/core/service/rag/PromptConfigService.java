package com.powertrade.core.service.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powertrade.core.model.ConfigVersionSummary;
import com.powertrade.core.model.PromptConfig;
import com.powertrade.core.service.config.ConfigVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 提示词配置服务
 */
@Service
public class PromptConfigService {

    private static final Logger log = LoggerFactory.getLogger(PromptConfigService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_MEMORY_ROUNDS = 6;
    private static final String CONFIG_TYPE = "prompt";
    private static final String CONFIG_KEY = "system";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ConfigVersionService configVersionService;

    @Value("${rag.prompt.config-file:./data/prompt-config.json}")
    private String configFilePath;

    private PromptConfig currentConfig;

    @PostConstruct
    public synchronized void init() {
        loadConfig();
    }

    public synchronized PromptConfig getConfig() {
        if (currentConfig == null) {
            loadConfig();
        }
        return copyOf(currentConfig);
    }

    public synchronized PromptConfig updateConfig(PromptConfig request) {
        PromptConfig nextConfig = mergeConfig(request);
        nextConfig.setUpdatedAt(LocalDateTime.now().format(TIME_FORMATTER));
        persistConfig(nextConfig);
        currentConfig = nextConfig;
        return copyOf(currentConfig);
    }

    public synchronized PromptConfig resetDefault() {
        PromptConfig defaultConfig = buildDefaultConfig();
        persistConfig(defaultConfig);
        currentConfig = defaultConfig;
        return copyOf(currentConfig);
    }

    public synchronized List<ConfigVersionSummary> listVersions() {
        return configVersionService.listVersions(CONFIG_TYPE, CONFIG_KEY);
    }

    public synchronized PromptConfig activateVersion(Integer versionNo) {
        PromptConfig config = configVersionService.activateVersion(CONFIG_TYPE, CONFIG_KEY, versionNo, PromptConfig.class);
        if (config == null) {
            throw new RuntimeException("指定版本不存在: " + versionNo);
        }
        currentConfig = sanitizeConfig(config);
        persistLegacyFile(currentConfig);
        return copyOf(currentConfig);
    }

    private void loadConfig() {
        PromptConfig dbConfig = configVersionService.getActiveConfig(CONFIG_TYPE, CONFIG_KEY, PromptConfig.class);
        if (dbConfig != null) {
            currentConfig = sanitizeConfig(dbConfig);
            return;
        }

        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            currentConfig = buildDefaultConfig();
            persistConfig(currentConfig);
            return;
        }

        try {
            currentConfig = sanitizeConfig(objectMapper.readValue(configFile, PromptConfig.class));
            persistConfig(currentConfig);
        } catch (IOException e) {
            log.error("读取提示词配置失败，使用默认配置: {}", e.getMessage(), e);
            currentConfig = buildDefaultConfig();
            persistConfig(currentConfig);
        }
    }

    private PromptConfig mergeConfig(PromptConfig request) {
        PromptConfig baseConfig = currentConfig == null ? buildDefaultConfig() : currentConfig;
        PromptConfig merged = new PromptConfig();

        merged.setAssistantName(defaultIfBlank(request.getAssistantName(), baseConfig.getAssistantName()));
        merged.setWelcomeMessage(defaultIfBlank(request.getWelcomeMessage(), baseConfig.getWelcomeMessage()));
        merged.setSystemPrompt(defaultIfBlank(request.getSystemPrompt(), baseConfig.getSystemPrompt()));
        merged.setFallbackReply(defaultIfBlank(request.getFallbackReply(), baseConfig.getFallbackReply()));
        merged.setMemoryRounds(normalizeMemoryRounds(request.getMemoryRounds(), baseConfig.getMemoryRounds()));

        return sanitizeConfig(merged);
    }

    private PromptConfig sanitizeConfig(PromptConfig config) {
        PromptConfig sanitized = new PromptConfig();
        sanitized.setAssistantName(defaultIfBlank(config.getAssistantName(), "小电"));
        sanitized.setWelcomeMessage(defaultIfBlank(
                config.getWelcomeMessage(),
                "您好，我是小电，很高兴为您服务。您可以咨询电力交易规则、政策解读、业务流程或知识库相关问题。"
        ));
        sanitized.setSystemPrompt(defaultIfBlank(config.getSystemPrompt(), defaultSystemPrompt()));
        sanitized.setFallbackReply(defaultIfBlank(
                config.getFallbackReply(),
                "抱歉，我暂时没有找到与您问题直接相关的信息。您可以换个问法，或补充更多背景后我继续帮您分析。"
        ));
        sanitized.setMemoryRounds(normalizeMemoryRounds(config.getMemoryRounds(), DEFAULT_MEMORY_ROUNDS));
        sanitized.setVersionNo(config.getVersionNo());
        sanitized.setConfigKey(defaultIfBlank(config.getConfigKey(), CONFIG_KEY));
        sanitized.setUpdatedAt(defaultIfBlank(config.getUpdatedAt(), LocalDateTime.now().format(TIME_FORMATTER)));
        return sanitized;
    }

    private PromptConfig buildDefaultConfig() {
        PromptConfig config = new PromptConfig();
        config.setAssistantName("小电");
        config.setWelcomeMessage("您好，我是小电，很高兴为您服务。您可以咨询电力交易规则、政策解读、业务流程或知识库相关问题。");
        config.setSystemPrompt(defaultSystemPrompt());
        config.setFallbackReply("抱歉，我暂时没有找到与您问题直接相关的信息。您可以换个问法，或补充更多背景后我继续帮您分析。");
        config.setMemoryRounds(DEFAULT_MEMORY_ROUNDS);
        config.setVersionNo(1);
        config.setConfigKey(CONFIG_KEY);
        config.setUpdatedAt(LocalDateTime.now().format(TIME_FORMATTER));
        return config;
    }

    private String defaultSystemPrompt() {
        return "你是一名电力交易领域的专业智能客服，名称为\"小电\"。\n"
                + "你的核心职责是为用户提供专业、可信、易懂的咨询服务。\n"
                + "请遵守以下要求：\n"
                + "1. 优先结合知识库检索结果作答，并保持回答准确、简洁、可执行。\n"
                + "2. 结合当前会话历史理解用户上下文，保持多轮对话连贯。\n"
                + "3. 当信息不足时，明确说明不确定性，并主动提出澄清问题。\n"
                + "4. 对政策、规则、流程类问题，优先给出结论，再补充原因和建议。\n"
                + "5. 语气保持客服式的友好、专业、耐心，不编造不存在的事实。";
    }

    private void persistConfig(PromptConfig config) {
        config.setConfigKey(CONFIG_KEY);
        Integer latestVersion = configVersionService.getLatestVersionNo(CONFIG_TYPE, CONFIG_KEY);
        config.setVersionNo(latestVersion + 1);
        configVersionService.saveAsNewActiveVersion(CONFIG_TYPE, CONFIG_KEY, config, "提示词配置变更", "system");
        persistLegacyFile(config);
    }

    private void persistLegacyFile(PromptConfig config) {
        File configFile = new File(configFilePath);
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("创建提示词配置目录失败: " + parent.getAbsolutePath());
        }

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config);
        } catch (IOException e) {
            throw new RuntimeException("保存提示词配置失败: " + e.getMessage(), e);
        }
    }

    private PromptConfig copyOf(PromptConfig source) {
        PromptConfig copy = new PromptConfig();
        copy.setAssistantName(source.getAssistantName());
        copy.setWelcomeMessage(source.getWelcomeMessage());
        copy.setSystemPrompt(source.getSystemPrompt());
        copy.setFallbackReply(source.getFallbackReply());
        copy.setMemoryRounds(source.getMemoryRounds());
        copy.setVersionNo(source.getVersionNo());
        copy.setConfigKey(source.getConfigKey());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private Integer normalizeMemoryRounds(Integer requested, Integer defaultValue) {
        int value = requested == null ? defaultValue : requested;
        if (value < 1) {
            return 1;
        }
        if (value > 20) {
            return 20;
        }
        return value;
    }
}
