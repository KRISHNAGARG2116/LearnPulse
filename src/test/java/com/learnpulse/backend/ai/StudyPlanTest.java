package com.learnpulse.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.DailyScheduleDto;
import com.learnpulse.backend.dto.StudyPlanRequest;
import com.learnpulse.backend.dto.StudyPlanResponse;
import com.learnpulse.backend.dto.TaskDto;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.service.StudyPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyPlanTest {

    @Mock
    private ChatClient chatClient;

    private PromptManager promptManager;
    private StructuredAiOutputParser structuredParser;
    private StudyPlanService studyPlanService;
    private User testStudent;

    @BeforeEach
    void setUp() {
        promptManager = new PromptManager();
        structuredParser = new StructuredAiOutputParser(new ObjectMapper().findAndRegisterModules());
        studyPlanService = new StudyPlanService(chatClient, promptManager, structuredParser);

        testStudent = User.builder()
                .id(UUID.randomUUID())
                .email("student@learnpulse.ai")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    @DisplayName("Should successfully generate a valid study plan bounded by available daily study hours")
    void testGenerateStudyPlanSuccess() {
        LocalDate futureExamDate = LocalDate.now().plusDays(10);
        StudyPlanRequest request = StudyPlanRequest.builder()
                .examDate(futureExamDate)
                .subjects(List.of("Java", "Data Structures"))
                .availableHoursPerDay(4.0)
                .build();

        String mockAiJson = "{\n" +
                "  \"examDate\": \"" + futureExamDate + "\",\n" +
                "  \"daysRemaining\": 10,\n" +
                "  \"availableHoursPerDay\": 4.0,\n" +
                "  \"plan\": [\n" +
                "    {\n" +
                "      \"date\": \"" + LocalDate.now().plusDays(1) + "\",\n" +
                "      \"tasks\": [\n" +
                "        {\"subject\": \"Java\", \"topic\": \"Collections\", \"hours\": 2.0},\n" +
                "        {\"subject\": \"Data Structures\", \"topic\": \"Binary Trees\", \"hours\": 2.0}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        ChatResponse mockResponse = new ChatResponse(List.of(new Generation(mockAiJson)));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        StudyPlanResponse response = studyPlanService.generateStudyPlan(testStudent, request);
        assertNotNull(response);
        assertEquals(futureExamDate, response.getExamDate());
        assertEquals(10, response.getDaysRemaining());
        assertEquals(4.0, response.getAvailableHoursPerDay());
        assertEquals(1, response.getPlan().size());
        assertEquals(2, response.getPlan().get(0).getTasks().size());
    }

    @Test
    @DisplayName("Should REJECT study plan when AI generated daily task hours exceed student's available daily hours limit")
    void testStudyPlanExceedingAvailableHoursRejected() {
        LocalDate futureExamDate = LocalDate.now().plusDays(5);
        StudyPlanRequest request = StudyPlanRequest.builder()
                .examDate(futureExamDate)
                .subjects(List.of("Java", "Algorithms"))
                .availableHoursPerDay(3.0) // Student only has 3.0 hours available per day
                .build();

        // AI returns 6.0 total hours allocated for a single day (exceeding 3.0 limit)
        String mockExceedingJson = "{\n" +
                "  \"examDate\": \"" + futureExamDate + "\",\n" +
                "  \"daysRemaining\": 5,\n" +
                "  \"availableHoursPerDay\": 3.0,\n" +
                "  \"plan\": [\n" +
                "    {\n" +
                "      \"date\": \"" + LocalDate.now().plusDays(1) + "\",\n" +
                "      \"tasks\": [\n" +
                "        {\"subject\": \"Java\", \"topic\": \"Multithreading\", \"hours\": 4.0},\n" +
                "        {\"subject\": \"Algorithms\", \"topic\": \"Dynamic Programming\", \"hours\": 2.0}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        ChatResponse mockResponse = new ChatResponse(List.of(new Generation(mockExceedingJson)));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        // Verify deterministic backend rejection
        ApiException exception = assertThrows(ApiException.class, () -> studyPlanService.generateStudyPlan(testStudent, request));
        assertTrue(exception.getMessage().contains("exceeded daily available study hours limit"));
    }

    @Test
    @DisplayName("Should throw ApiException for past exam date")
    void testPastExamDateThrowsApiException() {
        StudyPlanRequest request = StudyPlanRequest.builder()
                .examDate(LocalDate.now().minusDays(1))
                .subjects(List.of("Math"))
                .availableHoursPerDay(2.0)
                .build();

        assertThrows(ApiException.class, () -> studyPlanService.generateStudyPlan(testStudent, request));
    }

    @Test
    @DisplayName("Should throw ApiException for empty subjects list")
    void testEmptySubjectsThrowsApiException() {
        StudyPlanRequest request = StudyPlanRequest.builder()
                .examDate(LocalDate.now().plusDays(5))
                .subjects(Collections.emptyList())
                .availableHoursPerDay(2.0)
                .build();

        assertThrows(ApiException.class, () -> studyPlanService.generateStudyPlan(testStudent, request));
    }

    @Test
    @DisplayName("Should throw ApiException for invalid available study hours")
    void testInvalidStudyHoursThrowsApiException() {
        StudyPlanRequest request = StudyPlanRequest.builder()
                .examDate(LocalDate.now().plusDays(5))
                .subjects(List.of("Physics"))
                .availableHoursPerDay(0.0) // Invalid 0 hours
                .build();

        assertThrows(ApiException.class, () -> studyPlanService.generateStudyPlan(testStudent, request));
    }

    @Test
    @DisplayName("Should throw ApiException with HTTP 503 when ChatClient/Ollama service throws Exception")
    void testStudyPlanLlmFailureThrows503ServiceUnavailable() {
        StudyPlanRequest request = StudyPlanRequest.builder()
                .examDate(LocalDate.now().plusDays(5))
                .subjects(List.of("Math"))
                .availableHoursPerDay(3.0)
                .build();
        when(chatClient.call(any(Prompt.class))).thenThrow(new RuntimeException("Ollama service offline"));

        ApiException exception = assertThrows(ApiException.class, () -> studyPlanService.generateStudyPlan(testStudent, request));
        assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertTrue(exception.getMessage().contains("offline or unreachable"));
    }
}
