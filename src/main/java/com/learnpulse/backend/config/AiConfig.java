package com.learnpulse.backend.config;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model:qwen2.5-coder:7b}")
    private String ollamaModel;

    @Value("${spring.ai.ollama.chat.options.temperature:0.7}")
    private Double temperature;

    @Bean
    @Primary
    public ChatClient chatClient() {
        OllamaApi ollamaApi = new OllamaApi(ollamaBaseUrl);
        OllamaOptions options = OllamaOptions.create()
                .withModel(ollamaModel)
                .withTemperature(temperature != null ? temperature.floatValue() : 0.7f);
        return new OllamaChatClient(ollamaApi).withDefaultOptions(options);
    }
}
