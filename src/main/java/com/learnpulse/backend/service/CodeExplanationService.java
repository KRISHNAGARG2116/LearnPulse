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

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeExplanationService {

    private final ChatClient chatClient;
    private final PromptManager promptManager;
    private final StructuredAiOutputParser structuredParser;

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
        try {
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            rawOutput = (response != null && response.getResult() != null && response.getResult().getOutput() != null)
                    ? response.getResult().getOutput().getContent()
                    : null;
        } catch (Exception ex) {
            log.error("Ollama/ChatClient invocation failed for code explanation: {}", ex.getMessage(), ex);
            throw new ApiException("The AI code explanation service is currently offline or unreachable. Please try again later.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        return structuredParser.parseAndValidate(rawOutput, CodeExplanationResponse.class);
    }
}
