package com.learnpulse.backend.service;

import com.learnpulse.backend.ai.PromptManager;
import com.learnpulse.backend.ai.StructuredAiOutputParser;
import com.learnpulse.backend.config.AiLimitsConstants;
import com.learnpulse.backend.dto.CodeExplanationRequest;
import com.learnpulse.backend.dto.CodeExplanationResponse;
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
public class CodeExplanationService {

    private final ChatClient chatClient;
    private final PromptManager promptManager;
    private final StructuredAiOutputParser structuredParser;
    private final AiTelemetryService aiTelemetryService;

    @org.springframework.beans.factory.annotation.Autowired
    public CodeExplanationService(ChatClient chatClient,
                                  PromptManager promptManager,
                                  StructuredAiOutputParser structuredParser,
                                  @org.springframework.beans.factory.annotation.Autowired(required = false) AiTelemetryService aiTelemetryService) {
        this.chatClient = chatClient;
        this.promptManager = promptManager;
        this.structuredParser = structuredParser;
        this.aiTelemetryService = aiTelemetryService;
    }

    public CodeExplanationService(ChatClient chatClient,
                                  PromptManager promptManager,
                                  StructuredAiOutputParser structuredParser) {
        this(chatClient, promptManager, structuredParser, null);
    }

    public CodeExplanationResponse explainCode(User currentUser, CodeExplanationRequest request) {
        if (currentUser == null) {
            throw new ApiException("Authentication required to explain source code.", HttpStatus.UNAUTHORIZED);
        }

        if (request == null || !StringUtils.hasText(request.getCode())) {
            throw new ApiException("Source code is required and cannot be empty or blank.", HttpStatus.BAD_REQUEST);
        }

        String code = request.getCode().trim();
        if (code.length() > AiLimitsConstants.MAX_CODE_EXPLAIN_CHARS) {
            throw new ApiException("Source code exceeds maximum allowed length of " + AiLimitsConstants.MAX_CODE_EXPLAIN_CHARS + " characters.", HttpStatus.BAD_REQUEST);
        }

        log.info("Student {} requesting static code explanation (length: {} chars)", currentUser.getEmail(), code.length());

        Prompt prompt = promptManager.buildCodeExplanationPrompt(code, request.getLanguage());
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
                promptTokens = Math.max(1, code.length() / 4);
                compTokens = (rawOutput != null) ? Math.max(1, rawOutput.length() / 4) : 0;
                provenance = "LOCAL_ESTIMATION";
            }
            if (aiTelemetryService != null) {
                aiTelemetryService.recordOperation("CODE_EXPLAINER", "qwen2.5-coder:7b", currentUser, startTime, Instant.now(), "SUCCESS", promptTokens, compTokens, provenance, null);
            }
        } catch (Exception ex) {
            log.error("Ollama/ChatClient invocation failed for code explanation: {}", ex.getMessage(), ex);
            if (aiTelemetryService != null) {
                aiTelemetryService.recordOperation("CODE_EXPLAINER", "qwen2.5-coder:7b", currentUser, startTime, Instant.now(), "FAILED", Math.max(1, code.length() / 4), 0, "LOCAL_ESTIMATION", ex.getMessage());
            }
            throw new ApiException("The AI code explanation service is currently offline or unreachable. Please try again later.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        return structuredParser.parseAndValidate(rawOutput, CodeExplanationResponse.class);
    }
}
