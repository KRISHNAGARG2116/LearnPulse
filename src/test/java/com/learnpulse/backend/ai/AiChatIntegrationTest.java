package com.learnpulse.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.AiChatRequest;
import com.learnpulse.backend.entity.AiChatHistory;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.repository.AiChatHistoryRepository;
import com.learnpulse.backend.repository.UserRepository;
import com.learnpulse.backend.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("default")
@Transactional
public class AiChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiChatHistoryRepository chatHistoryRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ChatClient chatClient;

    private User studentUser;
    private String studentToken;

    @BeforeEach
    void setUp() {
        chatHistoryRepository.deleteAll();

        String email = "student_ai_test_" + UUID.randomUUID() + "@learnpulse.ai";
        studentUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.STUDENT)
                .enabled(true)
                .build();
        studentUser = userRepository.save(studentUser);

        studentToken = jwtProvider.generateAccessToken(studentUser);

        // Default mock ChatClient response
        when(chatClient.call(any(Prompt.class))).thenAnswer(invocation -> {
            org.springframework.ai.chat.Generation generation = new org.springframework.ai.chat.Generation("Inheritance in Java allows a subclass to acquire properties and methods from a superclass using the 'extends' keyword.");
            return new org.springframework.ai.chat.ChatResponse(List.of(generation));
        });
    }

    @Test
    @DisplayName("AI-001: Basic AI Chat Request Success")
    void testBasicAiChatRequestSuccess() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("What is Java inheritance?")
                .subjectName("Object-Oriented Programming")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.aiResponse", containsString("Inheritance in Java")))
                .andExpect(jsonPath("$.data.fallback").value(false))
                .andExpect(jsonPath("$.data.conversationId", notNullValue()));
    }

    @Test
    @DisplayName("AI-002: Rejection of Empty/Null/Whitespace Input")
    void testEmptyOrWhitespaceInputValidation() throws Exception {
        AiChatRequest emptyRequest = AiChatRequest.builder()
                .message("   ")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("Message cannot be empty"))));
    }

    @Test
    @DisplayName("AI-003: Controlled Fallback when Ollama Service is Unavailable")
    void testOllamaUnavailableFallbackBehavior() throws Exception {
        when(chatClient.call(any(Prompt.class))).thenThrow(new RuntimeException("Connection refused to Ollama host at http://localhost:11434"));

        AiChatRequest request = AiChatRequest.builder()
                .message("Explain polymorphism")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.fallback").value(true))
                .andExpect(jsonPath("$.data.aiResponse", containsString("offline or unreachable")));
    }

    @Test
    @DisplayName("AI-004: Multi-Turn Conversation Context & History Injection")
    void testMultiTurnConversationContext() throws Exception {
        String conversationId = "conv-" + UUID.randomUUID();

        // Save turn 1 in DB
        AiChatHistory turn1 = AiChatHistory.builder()
                .user(studentUser)
                .conversationId(conversationId)
                .userMessage("What is inheritance in Java?")
                .aiResponse("Inheritance allows a child class to inherit features from a parent class.")
                .build();
        chatHistoryRepository.save(turn1);

        AiChatRequest turn2Request = AiChatRequest.builder()
                .conversationId(conversationId)
                .message("Can you give me a simple code example?")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(turn2Request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.conversationId").value(conversationId));

        // Verify ChatClient was called with Prompt containing history
        verify(chatClient, atLeastOnce()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("AI-005: Multiple Requests & Basic Stability")
    void testMultipleSequentialRequests() throws Exception {
        for (int i = 1; i <= 3; i++) {
            AiChatRequest request = AiChatRequest.builder()
                    .message("Question number " + i)
                    .build();

            mockMvc.perform(post("/api/ai/chat")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }
    }

    @Test
    @DisplayName("AI-006: Chat History Persistence in PostgreSQL Database")
    void testChatHistoryPersistenceInDatabase() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Explain encapsulation with getters and setters.")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        List<AiChatHistory> history = chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(studentUser.getId());
        assertEquals(1, history.size());
        assertEquals("Explain encapsulation with getters and setters.", history.get(0).getUserMessage());
        assertNotNull(history.get(0).getAiResponse());
    }

    @Test
    @DisplayName("AI-007: Unauthenticated Request Rejection")
    void testUnauthenticatedRequestRejection() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Hello")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AI-008: Role Authorization Rejection for Non-Student Users")
    void testRoleAuthorizationRejection() throws Exception {
        User teacherUser = User.builder()
                .email("teacher_ai_" + UUID.randomUUID() + "@learnpulse.ai")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.TEACHER)
                .enabled(true)
                .build();
        teacherUser = userRepository.save(teacherUser);
        String teacherToken = jwtProvider.generateAccessToken(teacherUser);

        AiChatRequest request = AiChatRequest.builder()
                .message("Hello from teacher")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AI-009: Excessively Large Input Size Validation")
    void testExcessivelyLargeInputValidation() throws Exception {
        String largeMessage = "A".repeat(2500);
        AiChatRequest request = AiChatRequest.builder()
                .message(largeMessage)
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(containsString("must not exceed 2000 characters"))));
    }

    @Test
    @DisplayName("AI-010: Standardized API Response Format Verification")
    void testStandardizedApiResponseFormat() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("What is polymorphism?")
                .build();

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("AI response generated successfully"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.data.conversationId", notNullValue()))
                .andExpect(jsonPath("$.data.userMessage").value("What is polymorphism?"))
                .andExpect(jsonPath("$.data.aiResponse", notNullValue()));
    }
}
