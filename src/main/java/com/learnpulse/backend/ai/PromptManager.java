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

    private static final String RAG_SYSTEM_PROMPT = """
            You are a precise educational AI assistant for the LearnPulse learning platform.
            Answer the student's question using ONLY the provided document context below.
            
            Strict Guidelines:
            1. Base your answer strictly on the facts present in the provided Document Context.
            2. Do not invent, extrapolate, or use outside knowledge that is not directly supported by the context.
            3. Keep your explanation clear, educational, concise, and well-structured.
            4. If the provided context does not contain sufficient information to answer the question, state clearly: "The requested information could not be found in the provided document."
            """;

    /**
     * Builds a RAG prompt ensuring answer generation is strictly grounded in retrieved document context.
     *
     * @param contextText Retrieved document chunk context text
     * @param studentQuestion Student natural language question
     * @return Spring AI Prompt object with SystemMessage and UserMessage containing context & question
     */
    public Prompt buildRagPrompt(String contextText, String studentQuestion) {
        if (!StringUtils.hasText(studentQuestion)) {
            throw new IllegalArgumentException("Student question cannot be null or empty for RAG prompt.");
        }

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(RAG_SYSTEM_PROMPT));

        StringBuilder userMessageBuilder = new StringBuilder();
        userMessageBuilder.append("Document Context:\n-----------------\n");
        userMessageBuilder.append(StringUtils.hasText(contextText) ? contextText.trim() : "[No Relevant Document Context Provided]");
        userMessageBuilder.append("\n-----------------\n\nStudent Question:\n");
        userMessageBuilder.append(studentQuestion.trim());
        userMessageBuilder.append("\n\nProvide a clear and accurate document-grounded answer:");

        messages.add(new UserMessage(userMessageBuilder.toString()));
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
