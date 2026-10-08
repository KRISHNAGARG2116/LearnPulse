package com.learnpulse.backend.service;

import com.learnpulse.backend.ai.PromptManager;
import com.learnpulse.backend.ai.StructuredAiOutputParser;
import com.learnpulse.backend.config.AiLimitsConstants;
import com.learnpulse.backend.dto.FlashcardDto;
import com.learnpulse.backend.dto.FlashcardRequest;
import com.learnpulse.backend.dto.FlashcardResponse;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashcardGenerationService {

    private final ChatClient chatClient;
    private final PromptManager promptManager;
    private final StructuredAiOutputParser structuredParser;

    public FlashcardResponse generateFlashcards(User currentUser, FlashcardRequest request) {
        if (currentUser == null) {
            throw new ApiException("Authentication required to generate flashcards.", HttpStatus.UNAUTHORIZED);
        }

        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new ApiException("Educational content is required and cannot be empty or blank.", HttpStatus.BAD_REQUEST);
        }

        String content = request.getContent().trim();
        if (content.length() > AiLimitsConstants.MAX_FLASHCARDS_CHARS) {
            throw new ApiException("Educational content exceeds maximum allowed length of " + AiLimitsConstants.MAX_FLASHCARDS_CHARS + " characters.", HttpStatus.BAD_REQUEST);
        }

        int targetCardCount = (request.getCardCount() != null) ? request.getCardCount() : 5;
        if (targetCardCount < 1 || targetCardCount > 20) {
            throw new ApiException("Card count must be between 1 and 20.", HttpStatus.BAD_REQUEST);
        }

        log.info("Student {} requesting flashcards generation (content length: {} chars, target count: {})",
                currentUser.getEmail(), content.length(), targetCardCount);

        Prompt prompt = promptManager.buildFlashcardsPrompt(content, targetCardCount);
        String rawOutput;
        try {
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            rawOutput = (response != null && response.getResult() != null && response.getResult().getOutput() != null)
                    ? response.getResult().getOutput().getContent()
                    : null;
        } catch (Exception ex) {
            log.error("Ollama/ChatClient invocation failed for flashcard generation: {}", ex.getMessage(), ex);
            throw new ApiException("The AI flashcard generation service is currently offline or unreachable. Please try again later.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        FlashcardResponse response = structuredParser.parseAndValidate(rawOutput, FlashcardResponse.class);

        // Deterministic backend deduplication of flashcard questions
        List<FlashcardDto> deduplicatedCards = deduplicateFlashcards(response.getFlashcards());
        if (CollectionUtils.isEmpty(deduplicatedCards)) {
            throw new ApiException("The AI service generated empty or invalid flashcards.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setFlashcards(deduplicatedCards);
        return response;
    }

    /**
     * Deterministically filters out duplicate questions (case-insensitive & trimmed) from generated flashcards list.
     */
    private List<FlashcardDto> deduplicateFlashcards(List<FlashcardDto> cards) {
        if (CollectionUtils.isEmpty(cards)) {
            return Collections.emptyList();
        }

        Map<String, FlashcardDto> uniqueMap = new LinkedHashMap<>();
        for (FlashcardDto card : cards) {
            if (card == null || !StringUtils.hasText(card.getQuestion()) || !StringUtils.hasText(card.getAnswer())) {
                continue;
            }
            String normalizedKey = card.getQuestion().trim().toLowerCase();
            uniqueMap.putIfAbsent(normalizedKey, FlashcardDto.builder()
                    .question(card.getQuestion().trim())
                    .answer(card.getAnswer().trim())
                    .build());
        }

        return new ArrayList<>(uniqueMap.values());
    }
}
