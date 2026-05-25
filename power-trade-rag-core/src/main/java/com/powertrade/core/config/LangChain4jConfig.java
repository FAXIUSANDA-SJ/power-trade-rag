package com.powertrade.core.config;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * DashScope/Qwen 预留配置。
 * 当前仓库默认主线为 OpenAI，本类仅保留扩展入口，避免与主线 Bean 冲突。
 */
@Configuration
@ConditionalOnProperty(name = "rag.ai.provider", havingValue = "dashscope")
public class LangChain4jConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jConfig.class);

    @PostConstruct
    public void warnDashscopeNotImplemented() {
        log.warn("当前版本未启用 DashScope/Qwen 生产实现，请将 rag.ai.provider 切回 openai。");
    }
}
