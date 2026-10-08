package com.learnpulse.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.repository.UserRepository;
import com.learnpulse.backend.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AiFeaturesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @MockBean
    private ChatClient chatClient;

    private String studentToken;
    private User studentUser;
    private String teacherToken;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .email("student_ai_feat_" + UUID.randomUUID() + "@learnpulse.ai")
                .password("$2a$10$e846Mvg98KqE/h1.K8nK0.O9Ld9x5K0b5L2V6J5e0b5L2V6J5e0b5")
                .role(Role.STUDENT)
                .build();
        studentUser = userRepository.save(studentUser);

        studentToken = jwtProvider.generateAccessToken(studentUser);

        User teacherUser = User.builder()
                .email("teacher_ai_feat_" + UUID.randomUUID() + "@learnpulse.ai")
                .password("$2a$10$e846Mvg98KqE/h1.K8nK0.O9Ld9x5K0b5L2V6J5e0b5L2V6J5e0b5")
                .role(Role.TEACHER)
                .build();
        teacherUser = userRepository.save(teacherUser);

        teacherToken = jwtProvider.generateAccessToken(teacherUser);
    }

    @Test
    @DisplayName("POST /api/ai/summarize - Successful document summarization")
    void testSummarizeEndpointSuccess() throws Exception {
        SummarizeRequest request = SummarizeRequest.builder()
                .content("Object-Oriented Programming (OOP) is a programming paradigm based on the concept of objects.")
                .build();

        String mockAiJson = """
                {
                  "summary": "OOP is a programming paradigm focused on objects.",
                  "keyTakeaways": ["Object-oriented concept", "Class-based structure"],
                  "keywords": ["OOP", "Objects", "Classes"]
                }
                """;

        ChatResponse mockResponse = new ChatResponse(List.of(new Generation(mockAiJson)));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/ai/summarize")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.summary").value("OOP is a programming paradigm focused on objects."))
                .andExpect(jsonPath("$.data.keyTakeaways[0]").value("Object-oriented concept"))
                .andExpect(jsonPath("$.data.keywords[0]").value("OOP"));
    }

    @Test
    @DisplayName("POST /api/ai/explain-code - Successful static code explanation")
    void testExplainCodeEndpointSuccess() throws Exception {
        CodeExplanationRequest request = CodeExplanationRequest.builder()
                .code("public class Main { public static void main(String[] args) { System.out.println(\"Hello\"); } }")
                .language("Java")
                .build();

        String mockAiJson = """
                {
                  "purpose": "Prints Hello to standard output.",
                  "stepByStepLogic": ["Executes main method", "Prints string to console"],
                  "variables": ["args: Command-line arguments"],
                  "methods": ["main(String[] args)"],
                  "controlFlow": "Sequential execution.",
                  "algorithm": "Console Output",
                  "timeComplexity": "O(1)",
                  "spaceComplexity": "O(1)",
                  "potentialImprovements": ["Use a logging framework"]
                }
                """;

        ChatResponse mockResponse = new ChatResponse(List.of(new Generation(mockAiJson)));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/ai/explain-code")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.purpose").value("Prints Hello to standard output."))
                .andExpect(jsonPath("$.data.timeComplexity").value("O(1)"));
    }

    @Test
    @DisplayName("POST /api/ai/study-plan - Successful study plan generation")
    void testStudyPlanEndpointSuccess() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(7);
        StudyPlanRequest request = StudyPlanRequest.builder()
                .examDate(futureDate)
                .subjects(List.of("Java", "PostgreSQL"))
                .availableHoursPerDay(4.0)
                .build();

        String mockAiJson = "{\n" +
                "  \"examDate\": \"" + futureDate + "\",\n" +
                "  \"daysRemaining\": 7,\n" +
                "  \"availableHoursPerDay\": 4.0,\n" +
                "  \"plan\": [\n" +
                "    {\n" +
                "      \"date\": \"" + LocalDate.now().plusDays(1) + "\",\n" +
                "      \"tasks\": [\n" +
                "        {\"subject\": \"Java\", \"topic\": \"Generics\", \"hours\": 2.0},\n" +
                "        {\"subject\": \"PostgreSQL\", \"topic\": \"Indexes\", \"hours\": 2.0}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        ChatResponse mockResponse = new ChatResponse(List.of(new Generation(mockAiJson)));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/ai/study-plan")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.daysRemaining").value(7))
                .andExpect(jsonPath("$.data.plan[0].tasks[0].subject").value("Java"));
    }

    @Test
    @DisplayName("POST /api/ai/flashcards - Successful flashcards generation")
    void testFlashcardsEndpointSuccess() throws Exception {
        FlashcardRequest request = FlashcardRequest.builder()
                .content("Encapsulation hides internal implementation details. Abstraction shows only essential features.")
                .cardCount(2)
                .build();

        String mockAiJson = """
                {
                  "flashcards": [
                    {"question": "What is encapsulation?", "answer": "Hiding internal implementation details."},
                    {"question": "What is abstraction?", "answer": "Showing only essential features."}
                  ]
                }
                """;

        ChatResponse mockResponse = new ChatResponse(List.of(new Generation(mockAiJson)));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/ai/flashcards")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.flashcards.length()").value(2))
                .andExpect(jsonPath("$.data.flashcards[0].question").value("What is encapsulation?"));
    }

    @Test
    @DisplayName("Unauthenticated requests should return HTTP 401 Unauthorized")
    void testUnauthenticatedRequestsReturn401() throws Exception {
        SummarizeRequest request = SummarizeRequest.builder().content("Some document content").build();

        mockMvc.perform(post("/api/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Blank or invalid request bodies should return HTTP 400 Bad Request")
    void testInvalidRequestBodyReturns400() throws Exception {
        SummarizeRequest invalidRequest = SummarizeRequest.builder().content("   ").build();

        mockMvc.perform(post("/api/ai/summarize")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("Non-STUDENT authenticated role (TEACHER) should return HTTP 403 Forbidden for all AI endpoints")
    void testNonStudentAccessReturns403Forbidden() throws Exception {
        SummarizeRequest request = SummarizeRequest.builder().content("Valid document content for summarization").build();

        mockMvc.perform(post("/api/ai/summarize")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/ai/explain-code")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"int x = 1;\",\"language\":\"Java\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/ai/study-plan")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"examDate\":\"2026-10-15\",\"subjects\":[\"Java\"],\"availableHoursPerDay\":4.0}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/ai/flashcards")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Valid content for flashcards\",\"cardCount\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Ollama / LLM service failure should return clean HTTP 503 Service Unavailable")
    void testLlmServiceFailureReturns503ServiceUnavailable() throws Exception {
        SummarizeRequest request = SummarizeRequest.builder().content("Valid document content").build();

        when(chatClient.call(any(Prompt.class))).thenThrow(new RuntimeException("Ollama connection timed out or unreachable"));

        mockMvc.perform(post("/api/ai/summarize")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("The AI summarization service is currently offline or unreachable. Please try again later."));
    }
}
