package com.yzan.yzan_multi_agent.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public QwenEmbeddingModel qwenEmbeddingModel(
            @Value("${langchain4j.community.dashscope.embedding-model.api-key}") String apiKey,
            @Value("${langchain4j.community.dashscope.embedding-model.model-name}") String modelName
    ) {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl("https://dashscope.aliyuncs.com/api/v1")
                .build();
    }
}
