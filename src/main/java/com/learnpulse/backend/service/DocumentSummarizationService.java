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

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSummarizationService {

    private final ChatClient chatClient;
    private final PromptManager promptManager;
    private final StructuredAiOutputParser structuredParser;

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
        try {
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            rawOutput = (response != null && response.getResult() != null && response.getResult().getOutput() != null)
                    ? response.getResult().getOutput().getContent()
                    : null;
        } catch (Exception ex) {
            log.error("Ollama/ChatClient invocation failed for document summarization: {}", ex.getMessage(), ex);
            throw new ApiException("The AI summarization service is currently offline or unreachable. Please try again later.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        return structuredParser.parseAndValidate(rawOutput, SummaryResponse.class);
    }
}
