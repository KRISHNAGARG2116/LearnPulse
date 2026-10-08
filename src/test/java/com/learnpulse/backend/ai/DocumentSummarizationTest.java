package com.learnpulse.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.SummarizeRequest;
import com.learnpulse.backend.dto.SummaryResponse;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.service.DocumentSummarizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentSummarizationTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private PromptManager promptManager;

    private StructuredAiOutputParser structuredParser;
    private DocumentSummarizationService summarizationService;
    private User testStudent;

    @BeforeEach
    void setUp() {
        structuredParser = new StructuredAiOutputParser(new ObjectMapper());
        summarizationService = new DocumentSummarizationService(chatClient, promptManager, structuredParser);

        testStudent = User.builder()
                .id(UUID.randomUUID())
                .email("student@learnpulse.ai")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    @DisplayName("Should successfully summarize valid document content")
    void testSummarizeDocumentSuccess() {
        SummarizeRequest request = SummarizeRequest.builder()
                .content("Java is a high-level, class-based, object-oriented programming language designed to have as few implementation dependencies as possible.")
                .build();

        String mockAiJson = """
                {
                  "summary": "Java is an object-oriented programming language designed for portability.",
                  "keyTakeaways": ["Class-based language", "Object-oriented design"],
                  "keywords": ["Java", "OOP", "Programming"]
                }
                """;

        ChatResponse mockResponse = new ChatResponse(List.of(new Generation(mockAiJson)));

        when(promptManager.buildSummarizationPrompt(any())).thenReturn(new Prompt("test prompt"));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        SummaryResponse response = summarizationService.summarizeDocument(testStudent, request);
        assertNotNull(response);
        assertEquals("Java is an object-oriented programming language designed for portability.", response.getSummary());
        assertEquals(2, response.getKeyTakeaways().size());
        assertEquals(3, response.getKeywords().size());
    }

    @Test
    @DisplayName("Should throw ApiException for blank or empty content")
    void testSummarizeBlankContentThrowsException() {
        SummarizeRequest request = SummarizeRequest.builder().content("   ").build();
        assertThrows(ApiException.class, () -> summarizationService.summarizeDocument(testStudent, request));
    }

    @Test
    @DisplayName("Should throw ApiException for unauthenticated user")
    void testSummarizeUnauthenticatedUserThrowsException() {
        SummarizeRequest request = SummarizeRequest.builder().content("Valid content").build();
        assertThrows(ApiException.class, () -> summarizationService.summarizeDocument(null, request));
    }

    @Test
    @DisplayName("Should throw ApiException for oversized content exceeding 20,000 chars")
    void testSummarizeOversizedContentThrowsException() {
        String oversized = "a".repeat(20001);
        SummarizeRequest request = SummarizeRequest.builder().content(oversized).build();
        assertThrows(ApiException.class, () -> summarizationService.summarizeDocument(testStudent, request));
    }

    @Test
    @DisplayName("Should throw ApiException with HTTP 503 when ChatClient/Ollama service throws Exception")
    void testSummarizeLlmFailureThrows503ServiceUnavailable() {
        SummarizeRequest request = SummarizeRequest.builder().content("Valid document content").build();
        when(promptManager.buildSummarizationPrompt(any())).thenReturn(new Prompt("test prompt"));
        when(chatClient.call(any(Prompt.class))).thenThrow(new RuntimeException("Ollama service unreachable"));

        ApiException exception = assertThrows(ApiException.class, () -> summarizationService.summarizeDocument(testStudent, request));
        assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertTrue(exception.getMessage().contains("offline or unreachable"));
    }
}
