package com.learnpulse.backend.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.DocumentRagRequest;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.UploadedDocument;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.repository.AiChatHistoryRepository;
import com.learnpulse.backend.repository.UploadedDocumentRepository;
import com.learnpulse.backend.repository.UserRepository;
import com.learnpulse.backend.security.jwt.JwtProvider;
import com.learnpulse.backend.service.DocumentChunkingService;
import com.learnpulse.backend.service.DocumentRetrievalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("default")
@Transactional
public class DocumentRagIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UploadedDocumentRepository documentRepository;

    @Autowired
    private AiChatHistoryRepository aiChatHistoryRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ChatClient chatClient;

    @MockBean
    private DocumentRetrievalService retrievalService;

    @MockBean
    private DocumentChunkingService chunkingService;

    private User studentUser;
    private User teacherUser;
    private String studentToken;
    private String teacherToken;
    private UploadedDocument testDocument;

    @BeforeEach
    public void setUp() {
        aiChatHistoryRepository.deleteAll();

        studentUser = User.builder()
                .email("rag_student_" + UUID.randomUUID() + "@learnpulse.ai")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.STUDENT)
                .enabled(true)
                .build();
        studentUser = userRepository.save(studentUser);

        teacherUser = User.builder()
                .email("rag_teacher_" + UUID.randomUUID() + "@learnpulse.ai")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.TEACHER)
                .enabled(true)
                .build();
        teacherUser = userRepository.save(teacherUser);

        studentToken = jwtProvider.generateAccessToken(studentUser);
        teacherToken = jwtProvider.generateAccessToken(teacherUser);

        testDocument = documentRepository.save(UploadedDocument.builder()
                .originalFileName("Java_Fundamentals_Spec.pdf")
                .storedFileName("uuid_java_spec_" + UUID.randomUUID() + ".pdf")
                .filePath("uploads/uuid_java_spec.pdf")
                .fileSize(2048L)
                .contentType("application/pdf")
                .teacher(teacherUser)
                .active(true)
                .extractedText("Inheritance is a fundamental concept in Java where a child class acquires properties of a parent class using the extends keyword.")
                .build());

        when(chunkingService.chunkDocument(any())).thenReturn(Collections.emptyList());
    }

    @AfterEach
    public void tearDown() {
        aiChatHistoryRepository.deleteAll();
    }

    @Test
    @DisplayName("RAG-API-001: Successful Student Document RAG Request & Grounded Answer")
    public void testSuccessfulDocumentRagRequest() throws Exception {
        Map<String, Object> meta = new HashMap<>();
        meta.put("documentId", testDocument.getId().toString());
        meta.put("fileName", "Java_Fundamentals_Spec.pdf");
        meta.put("chunkIndex", 0);
        meta.put("distance", 0.12);

        Document matchedChunk = new Document("Inheritance is a fundamental concept in Java where a child class acquires properties of a parent class using the extends keyword.", meta);

        when(retrievalService.retrieveRelevantChunks(eq("What is inheritance?"), eq(testDocument.getId()), anyInt(), anyDouble()))
                .thenReturn(Collections.singletonList(matchedChunk));

        Generation generation = new Generation("Inheritance in Java allows a child class to inherit fields and methods from a parent class using the extends keyword.");
        ChatResponse mockChatResponse = new ChatResponse(List.of(generation));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockChatResponse);

        DocumentRagRequest request = DocumentRagRequest.builder()
                .documentId(testDocument.getId())
                .question("What is inheritance?")
                .build();

        mockMvc.perform(post("/api/ai/ask-document")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.documentId").value(testDocument.getId().toString()))
                .andExpect(jsonPath("$.data.question").value("What is inheritance?"))
                .andExpect(jsonPath("$.data.answer", containsString("child class")))
                .andExpect(jsonPath("$.data.fallback").value(false))
                .andExpect(jsonPath("$.data.sources", hasSize(1)))
                .andExpect(jsonPath("$.data.sources[0].fileName").value("Java_Fundamentals_Spec.pdf"));
    }

    @Test
    @DisplayName("RAG-API-002: No-Context Response When Similarity Search Yields No Match")
    public void testNoContextResponse() throws Exception {
        when(retrievalService.retrieveRelevantChunks(eq("What is quantum mechanics?"), eq(testDocument.getId()), anyInt(), anyDouble()))
                .thenReturn(Collections.emptyList());

        DocumentRagRequest request = DocumentRagRequest.builder()
                .documentId(testDocument.getId())
                .question("What is quantum mechanics?")
                .build();

        mockMvc.perform(post("/api/ai/ask-document")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.answer").value("The requested information could not be found in the selected document."))
                .andExpect(jsonPath("$.data.sources", hasSize(0)))
                .andExpect(jsonPath("$.data.fallback").value(false));
    }

    @Test
    @DisplayName("RAG-API-003: Invalid Document ID Returns 404 Not Found")
    public void testInvalidDocumentIdRejection() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        DocumentRagRequest request = DocumentRagRequest.builder()
                .documentId(nonExistentId)
                .question("What is inheritance?")
                .build();

        mockMvc.perform(post("/api/ai/ask-document")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message", containsString("Document not found")));
    }

    @Test
    @DisplayName("RAG-API-004: Blank Question Returns 400 Bad Request")
    public void testBlankQuestionValidation() throws Exception {
        DocumentRagRequest request = DocumentRagRequest.builder()
                .documentId(testDocument.getId())
                .question("   ")
                .build();

        mockMvc.perform(post("/api/ai/ask-document")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("RAG-API-005: LLM Failure Controlled Fallback Handling")
    public void testLlmFailureFallback() throws Exception {
        Map<String, Object> meta = new HashMap<>();
        meta.put("documentId", testDocument.getId().toString());
        meta.put("fileName", "Java_Fundamentals_Spec.pdf");
        Document matchedChunk = new Document("Inheritance details...", meta);

        when(retrievalService.retrieveRelevantChunks(eq("Explain inheritance"), eq(testDocument.getId()), anyInt(), anyDouble()))
                .thenReturn(Collections.singletonList(matchedChunk));

        when(chatClient.call(any(Prompt.class))).thenThrow(new RuntimeException("Ollama service unreachable"));

        DocumentRagRequest request = DocumentRagRequest.builder()
                .documentId(testDocument.getId())
                .question("Explain inheritance")
                .build();

        mockMvc.perform(post("/api/ai/ask-document")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fallback").value(true))
                .andExpect(jsonPath("$.data.answer", containsString("AI service is currently unavailable")));
    }

    @Test
    @DisplayName("RAG-API-006: Unauthenticated Request Returns 401 Unauthorized")
    public void testUnauthenticatedRequestRejection() throws Exception {
        DocumentRagRequest request = DocumentRagRequest.builder()
                .documentId(testDocument.getId())
                .question("What is inheritance?")
                .build();

        mockMvc.perform(post("/api/ai/ask-document")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RAG-API-007: Non-Student Role Guard Returns 403 Forbidden")
    public void testNonStudentRoleGuardRejection() throws Exception {
        DocumentRagRequest request = DocumentRagRequest.builder()
                .documentId(testDocument.getId())
                .question("What is inheritance?")
                .build();

        mockMvc.perform(post("/api/ai/ask-document")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
