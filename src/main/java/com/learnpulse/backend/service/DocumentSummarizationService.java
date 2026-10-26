package com.learnpulse.backend.service;

import com.learnpulse.backend.ai.PromptManager;
import com.learnpulse.backend.ai.StructuredAiOutputParser;
import com.learnpulse.backend.config.AiLimitsConstants;
import com.learnpulse.backend.dto.SummarizeRequest;
import com.learnpulse.backend.dto.SummaryResponse;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Slf4j
@Service
public class DocumentSummarizationService {

    private final ChatClient chatClient;
    private final PromptManager promptManager;
    private final StructuredAiOutputParser structuredParser;
    private final AiTelemetryService aiTelemetryService;

    @org.springframework.beans.factory.annotation.Autowired
    public DocumentSummarizationService(ChatClient chatClient,
                                       PromptManager promptManager,
                                       StructuredAiOutputParser structuredParser,
                                       @org.springframework.beans.factory.annotation.Autowired(required = false) AiTelemetryService aiTelemetryService) {
        this.chatClient = chatClient;
        this.promptManager = promptManager;
        this.structuredParser = structuredParser;
        this.aiTelemetryService = aiTelemetryService;
    }

    public DocumentSummarizationService(ChatClient chatClient,
                                       PromptManager promptManager,
                                       StructuredAiOutputParser structuredParser) {
        this(chatClient, promptManager, structuredParser, null);
    }

    public SummaryResponse summarizeDocument(User currentUser, SummarizeRequest request) {
        if (currentUser == null) {
            throw new ApiException("Authentication required to summarize documents.", HttpStatus.UNAUTHORIZED);
        }

        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new ApiException("Document content is required and cannot be empty or blank.", HttpStatus.BAD_REQUEST);
        }

        String content = request.getContent().trim();
        if (content.length() > AiLimitsConstants.MAX_SUMMARIZE_CHARS) {
            throw new ApiException("Document content exceeds maximum allowed length of " + AiLimitsConstants.MAX_SUMMARIZE_CHARS + " characters.", HttpStatus.BAD_REQUEST);
        }

        log.info("Student {} summarizing document content (length: {} chars)", currentUser.getEmail(), content.length());

        Prompt prompt = promptManager.buildSummarizationPrompt(content);
        String rawOutput;
        Instant startTime = Instant.now();
        try {
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            rawOutput = (response != null && response.getResult() != null && response.getResult().getOutput() != null)
                    ? response.getResult().getOutput().getContent()
                    : null;
            int promptTokens;
            int compTokens;
            String provenance;
            if (response != null && response.getMetadata() != null && response.getMetadata().getUsage() != null
                    && response.getMetadata().getUsage().getPromptTokens() != null && response.getMetadata().getUsage().getPromptTokens() > 0) {
                promptTokens = response.getMetadata().getUsage().getPromptTokens().intValue();
                compTokens = response.getMetadata().getUsage().getGenerationTokens() != null ? response.getMetadata().getUsage().getGenerationTokens().intValue() : 0;
                provenance = "PROVIDER_REPORTED";
            } else {
                promptTokens = Math.max(1, content.length() / 4);
                compTokens = (rawOutput != null) ? Math.max(1, rawOutput.length() / 4) : 0;
                provenance = "LOCAL_ESTIMATION";
            }
            if (aiTelemetryService != null) {
                aiTelemetryService.recordOperation("SUMMARIZATION", "qwen2.5-coder:7b", currentUser, startTime, Instant.now(), "SUCCESS", promptTokens, compTokens, provenance, null);
            }
        } catch (Exception ex) {
            log.error("Ollama/ChatClient invocation failed for document summarization: {}", ex.getMessage(), ex);
            if (aiTelemetryService != null) {
                aiTelemetryService.recordOperation("SUMMARIZATION", "qwen2.5-coder:7b", currentUser, startTime, Instant.now(), "FAILED", Math.max(1, content.length() / 4), 0, "LOCAL_ESTIMATION", ex.getMessage());
            }
            throw new ApiException("The AI summarization service is currently offline or unreachable. Please try again later.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        return structuredParser.parseAndValidate(rawOutput, SummaryResponse.class);
    }
}
