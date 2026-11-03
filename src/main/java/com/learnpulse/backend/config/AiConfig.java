package com.learnpulse.backend.config;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * AI subsystem configuration — configures the Ollama ChatClient with explicit
 * HTTP connect and read timeouts to prevent indefinite thread hangs when the
 * Ollama service is unavailable or slow.
 *
 * <p>Security note (SEC-01 Week 14): Without explicit timeouts, a hanging Ollama
 * process would exhaust the application thread pool. These timeouts ensure
 * controlled failure within bounded time, enabling the service-layer fallback
 * paths to activate correctly.</p>
 */
@Configuration
public class AiConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model:qwen2.5-coder:7b}")
    private String ollamaModel;

    @Value("${spring.ai.ollama.chat.options.temperature:0.7}")
    private Double temperature;

    /**
     * Ollama HTTP connect timeout in milliseconds.
     * Default: 10 seconds — fail fast if service is unreachable.
     */
    @Value("${ollama.connect-timeout-ms:10000}")
    private int ollamaConnectTimeoutMs;

    /**
     * Ollama HTTP read timeout in milliseconds.
     * Default: 120 seconds — LLM inference may be slow on CPU-only hosts.
     */
    @Value("${ollama.read-timeout-ms:120000}")
    private int ollamaReadTimeoutMs;

    @Bean
    @Primary
    public ChatClient chatClient() {
        // Configure RestClient with explicit connect and read timeouts.
        // This prevents indefinite thread blocking when Ollama is unreachable or
        // when a model inference request hangs (SEC-01 remediation).
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofMillis(ollamaConnectTimeoutMs).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofMillis(ollamaReadTimeoutMs).toMillis());

        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(requestFactory);

        OllamaApi ollamaApi = new OllamaApi(ollamaBaseUrl, restClientBuilder);

        OllamaOptions options = OllamaOptions.create()
                .withModel(ollamaModel)
                .withTemperature(temperature != null ? temperature.floatValue() : 0.7f);

        return new OllamaChatClient(ollamaApi).withDefaultOptions(options);
    }
}
