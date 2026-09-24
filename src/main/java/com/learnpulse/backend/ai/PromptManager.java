package com.learnpulse.backend.ai;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class PromptManager {

    private static final String DEFAULT_ACADEMIC_SYSTEM_PROMPT = """
            You are a supportive, knowledgeable academic tutor for the LearnPulse learning platform.
            Your role is to assist students in understanding educational concepts clearly.
            
            Guidelines:
            1. Be encouraging, patient, and clear.
            2. Explain concepts step-by-step using simple, relatable examples.
            3. Stay strictly within an educational context.
            4. Keep responses concise and structured.
            5. If a student's question is vague or ambiguous, politely ask for clarification.
            6. Do not present unverified or unsupported speculation as factual certainty.
            """;

    /**
     * Builds a Spring AI Prompt object containing system instructions, multi-turn history, and current user input.
     *
     * @param userMessage Current user query text
     * @param subjectName Optional subject context name
     * @param historyList Previous chat history records for multi-turn context (ordered chronological asc)
     * @return Spring AI Prompt object
     */
    public Prompt buildAcademicTutorPrompt(String userMessage, String subjectName, List<Message> historyList) {
        if (!StringUtils.hasText(userMessage)) {
            throw new IllegalArgumentException("User message prompt text cannot be null or empty.");
        }

        List<Message> messages = new ArrayList<>();

        // 1. Build System Instruction Message
        StringBuilder systemPromptBuilder = new StringBuilder(DEFAULT_ACADEMIC_SYSTEM_PROMPT);
        if (StringUtils.hasText(subjectName)) {
            systemPromptBuilder.append("\nNote: The current subject area focus is: ").append(subjectName.trim()).append(".");
        }
        messages.add(new SystemMessage(systemPromptBuilder.toString()));

        // 2. Inject Previous Conversation History (Multi-Turn)
        if (historyList != null && !historyList.isEmpty()) {
            messages.addAll(historyList);
        }

        // 3. Append Current User Message
        messages.add(new UserMessage(userMessage.trim()));

        return new Prompt(messages);
    }

    /**
     * Helper to create an AssistantMessage for chat history turns.
     */
    public Message createAssistantMessage(String content) {
        return new AssistantMessage(content);
    }

    /**
     * Helper to create a UserMessage for chat history turns.
     */
    public Message createUserMessage(String content) {
        return new UserMessage(content);
    }
}
