package com.learnpulse.backend.service;

import com.learnpulse.backend.ai.PromptManager;
import com.learnpulse.backend.dto.AiChatRequest;
import com.learnpulse.backend.dto.AiChatResponse;
import com.learnpulse.backend.entity.AiChatHistory;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.repository.AiChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiLlmService {

    private final ChatClient chatClient;
    private final PromptManager promptManager;
    private final AiChatHistoryRepository chatHistoryRepository;

    private static final String FALLBACK_MESSAGE = 
            "The AI tutor service is currently offline or unreachable. " +
            "Please verify that the local Ollama service and Qwen model are running, then try again.";

    /**
     * Processes an AI chat request for an authenticated student user.
     *
     * @param request AI chat request DTO
     * @param currentUser Authenticated User entity
     * @return AiChatResponse containing generated AI text or controlled fallback response
     */
    @Transactional
    public AiChatResponse processChatRequest(AiChatRequest request, User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalArgumentException("Authenticated user context is required for AI chat.");
        }

        String userMessage = request.getMessage() != null ? request.getMessage().trim() : "";
        if (!StringUtils.hasText(userMessage)) {
            throw new IllegalArgumentException("Message text cannot be empty or blank.");
        }

        // Generate or retain conversation ID
        String conversationId = StringUtils.hasText(request.getConversationId()) 
                ? request.getConversationId().trim() 
                : UUID.randomUUID().toString();

        // 1. Retrieve prior multi-turn conversation history for THIS user and THIS conversation ID only
        List<Message> historyMessages = buildConversationHistoryMessages(currentUser.getId(), conversationId);

        // 2. Build system + history + user prompt
        Prompt prompt = promptManager.buildAcademicTutorPrompt(userMessage, request.getSubjectName(), historyMessages);

        String aiResponseText;
        boolean isFallback = false;

        try {
            log.info("Dispatching prompt to ChatClient/Ollama for userId: {}, conversationId: {}", currentUser.getId(), conversationId);
            
            // Call Spring AI ChatClient with prompt
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            String modelOutput = (response != null && response.getResult() != null && response.getResult().getOutput() != null)
                    ? response.getResult().getOutput().getContent()
                    : null;
            
            if (StringUtils.hasText(modelOutput)) {
                aiResponseText = modelOutput.trim();
            } else {
                log.warn("Received empty/null response from LLM for conversationId: {}", conversationId);
                aiResponseText = FALLBACK_MESSAGE;
                isFallback = true;
            }
        } catch (Exception ex) {
            log.error("AI service error during ChatClient invocation for conversationId {}: {}", conversationId, ex.getMessage(), ex);
            aiResponseText = FALLBACK_MESSAGE;
            isFallback = true;
        }

        // 3. Persist successful interaction (or non-fatal history record) to database
        if (!isFallback) {
            try {
                AiChatHistory chatRecord = AiChatHistory.builder()
                        .user(currentUser)
                        .conversationId(conversationId)
                        .userMessage(userMessage)
                        .aiResponse(aiResponseText)
                        .createdAt(Instant.now())
                        .build();
                chatHistoryRepository.save(chatRecord);
                log.info("Persisted AI chat history record ID: {} for user: {}", chatRecord.getId(), currentUser.getId());
            } catch (Exception dbEx) {
                log.error("Failed to persist AI chat history record: {}", dbEx.getMessage(), dbEx);
            }
        }

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .userMessage(userMessage)
                .aiResponse(aiResponseText)
                .timestamp(Instant.now().toString())
                .fallback(isFallback)
                .build();
    }

    /**
     * Builds chronological list of Spring AI Message turns for prior conversation history.
     */
    private List<Message> buildConversationHistoryMessages(UUID userId, String conversationId) {
        try {
            // Retrieve last 10 records descending to prevent prompt explosion, then reverse to chronological asc order
            List<AiChatHistory> historyRecords = chatHistoryRepository.findTop10ByUserIdAndConversationIdOrderByCreatedAtDesc(userId, conversationId);
            if (historyRecords == null || historyRecords.isEmpty()) {
                return Collections.emptyList();
            }

            List<AiChatHistory> chronologicalList = new ArrayList<>(historyRecords);
            Collections.reverse(chronologicalList);

            List<Message> historyMessages = new ArrayList<>();
            for (AiChatHistory record : chronologicalList) {
                if (StringUtils.hasText(record.getUserMessage())) {
                    historyMessages.add(promptManager.createUserMessage(record.getUserMessage()));
                }
                if (StringUtils.hasText(record.getAiResponse())) {
                    historyMessages.add(promptManager.createAssistantMessage(record.getAiResponse()));
                }
            }
            return historyMessages;
        } catch (Exception ex) {
            log.warn("Unable to fetch conversation history for user: {}, conversationId: {}. Exception: {}", userId, conversationId, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
