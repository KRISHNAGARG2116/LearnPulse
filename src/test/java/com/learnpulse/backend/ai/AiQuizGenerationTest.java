package com.learnpulse.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.GenerateQuizRequest;
import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.repository.*;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AiQuizGenerationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private UploadedDocumentRepository documentRepository;

    @Autowired
    private TeacherSubjectRepository teacherSubjectRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private StudentQuizResultRepository resultRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private NotesRepository notesRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @MockBean
    private ChatClient chatClient;

    private User teacher;
    private String teacherToken;

    private User admin;
    private String adminToken;

    private User student;
    private String studentToken;

    private Subject subject;
    private Chapter chapter;
    private UploadedDocument document;

    @BeforeEach
    void setUp() {
        // Safe database cleanup in FK order
        resultRepository.deleteAll();
        questionRepository.deleteAll();
        quizRepository.deleteAll();
        notesRepository.deleteAll();
        documentRepository.deleteAll();
        studentEnrollmentRepository.deleteAll();
        teacherSubjectRepository.deleteAll();
        chapterRepository.deleteAll();
        subjectRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Teacher
        teacher = User.builder()
                .email("teacher_quiz_" + UUID.randomUUID() + "@learnpulse.ai")
                .password("$2a$10$e846Mvg98KqE/h1.K8nK0.O9Ld9x5K0b5L2V6J5e0b5L2V6J5e0b5")
                .role(Role.TEACHER)
                .build();
        teacher = userRepository.save(teacher);
        teacherToken = jwtProvider.generateAccessToken(teacher);

        // 2. Create Admin
        admin = User.builder()
                .email("admin_quiz_" + UUID.randomUUID() + "@learnpulse.ai")
                .password("$2a$10$e846Mvg98KqE/h1.K8nK0.O9Ld9x5K0b5L2V6J5e0b5L2V6J5e0b5")
                .role(Role.ADMIN)
                .build();
        admin = userRepository.save(admin);
        adminToken = jwtProvider.generateAccessToken(admin);

        // 3. Create Student
        student = User.builder()
                .email("student_quiz_" + UUID.randomUUID() + "@learnpulse.ai")
                .password("$2a$10$e846Mvg98KqE/h1.K8nK0.O9Ld9x5K0b5L2V6J5e0b5L2V6J5e0b5")
                .role(Role.STUDENT)
                .build();
        student = userRepository.save(student);
        studentToken = jwtProvider.generateAccessToken(student);

        // 4. Create Academic Subject & Chapter
        subject = Subject.builder()
                .name("Computer Networks")
                .code("CS301-" + UUID.randomUUID().toString().substring(0, 4))
                .description("Fundamental Networking Concepts")
                .build();
        subject = subjectRepository.save(subject);

        // Assign teacher to subject
        TeacherSubject teacherSubject = TeacherSubject.builder()
                .teacher(teacher)
                .subject(subject)
                .build();
        teacherSubjectRepository.save(teacherSubject);

        chapter = Chapter.builder()
                .title("OSI Reference Model")
                .chapterNumber(1)
                .subject(subject)
                .build();
        chapter = chapterRepository.save(chapter);

        // 5. Create Document with extracted text
        document = UploadedDocument.builder()
                .originalFileName("osi_model_guide.pdf")
                .storedFileName("osi_" + UUID.randomUUID() + ".pdf")
                .filePath("/tmp/osi.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .teacher(teacher)
                .subject(subject)
                .chapter(chapter)
                .active(true)
                .processingStatus(ProcessingStatus.PROCESSED)
                .extractedText("The OSI model consists of 7 layers: Physical, Data Link, Network, Transport, Session, Presentation, and Application. " +
                        "The Network layer is responsible for packet forwarding including routing through intermediate routers. " +
                        "The Transport layer provides end-to-end communication services for applications.")
                .build();
        document = documentRepository.save(document);
    }

    private String getValidAiQuizJson() {
        return """
                {
                  "title": "OSI Reference Model Assessment",
                  "questions": [
                    {
                      "questionText": "How many layers are in the OSI reference model?",
                      "optionA": "5 layers",
                      "optionB": "6 layers",
                      "optionC": "7 layers",
                      "optionD": "4 layers",
                      "correctAnswer": "C",
                      "explanation": "The OSI model consists of seven distinct layers.",
                      "difficulty": "BEGINNER",
                      "marks": 1
                    },
                    {
                      "questionText": "Which layer is responsible for packet routing through intermediate routers?",
                      "optionA": "Physical Layer",
                      "optionB": "Data Link Layer",
                      "optionC": "Network Layer",
                      "optionD": "Session Layer",
                      "correctAnswer": "C",
                      "explanation": "The Network layer handles logical addressing and packet forwarding across routers.",
                      "difficulty": "INTERMEDIATE",
                      "marks": 1
                    }
                  ]
                }
                """;
    }

    @Test
    @DisplayName("Day 67: POST /api/ai/generate-quiz - Successful quiz generation and persistence in DRAFT status by Teacher")
    void testTeacherGenerateQuizSuccess() throws Exception {
        Generation generation = new Generation(getValidAiQuizJson());
        ChatResponse mockResponse = new ChatResponse(java.util.List.of(generation));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .documentId(document.getId())
                .questionCount(2)
                .difficulty("INTERMEDIATE")
                .quizTitle("Custom OSI Model Quiz")
                .build();

        mockMvc.perform(post("/api/ai/generate-quiz")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.title").value("Custom OSI Model Quiz"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.isPublished").value(false))
                .andExpect(jsonPath("$.data.questionsCount").value(2))
                .andExpect(jsonPath("$.data.totalMarks").value(2))
                .andExpect(jsonPath("$.data.questions[0].questionText").value("How many layers are in the OSI reference model?"))
                .andExpect(jsonPath("$.data.questions[0].correctAnswer").value("C"))
                .andExpect(jsonPath("$.data.questions[0].explanation").isNotEmpty());

        // Verify that the quiz was persisted in the DB with status DRAFT
        var savedQuizzes = quizRepository.findBySubjectId(subject.getId());
        assertEquals(1, savedQuizzes.size());
        Quiz savedQuiz = savedQuizzes.get(0);
        assertEquals(QuizStatus.DRAFT, savedQuiz.getStatus());
        assertFalse(savedQuiz.getIsPublished());
        List<Question> savedQuestions = questionRepository.findByQuizId(savedQuiz.getId());
        assertEquals(2, savedQuestions.size());
        assertEquals(QuestionSource.AI_GENERATED, savedQuestions.get(0).getSource());
    }

    @Test
    @DisplayName("Day 67: POST /api/ai/generate-quiz - Admin can generate quiz for any document")
    void testAdminGenerateQuizSuccess() throws Exception {
        Generation generation = new Generation(getValidAiQuizJson());
        ChatResponse mockResponse = new ChatResponse(java.util.List.of(generation));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .documentId(document.getId())
                .questionCount(2)
                .build();

        mockMvc.perform(post("/api/ai/generate-quiz")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.questionsCount").value(2));
    }

    @Test
    @DisplayName("Day 67: POST /api/ai/generate-quiz - Student is FORBIDDEN (HTTP 403)")
    void testStudentForbiddenFromGeneratingQuiz() throws Exception {
        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .documentId(document.getId())
                .questionCount(5)
                .build();

        mockMvc.perform(post("/api/ai/generate-quiz")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Day 67: POST /api/ai/generate-quiz - Teacher cannot generate quiz for document in unassigned subject (HTTP 403)")
    void testTeacherForbiddenForUnassignedSubjectDocument() throws Exception {
        // Create another teacher who is NOT assigned to this subject
        User anotherTeacher = User.builder()
                .email("other_teacher_" + UUID.randomUUID() + "@learnpulse.ai")
                .password("$2a$10$e846Mvg98KqE/h1.K8nK0.O9Ld9x5K0b5L2V6J5e0b5L2V6J5e0b5")
                .role(Role.TEACHER)
                .build();
        anotherTeacher = userRepository.save(anotherTeacher);
        String otherToken = jwtProvider.generateAccessToken(anotherTeacher);

        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .documentId(document.getId())
                .questionCount(5)
                .build();

        mockMvc.perform(post("/api/ai/generate-quiz")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Day 67: POST /api/ai/generate-quiz - Missing document returns HTTP 404")
    void testMissingDocumentReturns404() throws Exception {
        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .documentId(UUID.randomUUID())
                .questionCount(5)
                .build();

        mockMvc.perform(post("/api/ai/generate-quiz")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Day 67: POST /api/ai/generate-quiz - Invalid question count returns HTTP 400")
    void testInvalidQuestionCountReturns400() throws Exception {
        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .documentId(document.getId())
                .questionCount(25) // Max allowed is 20
                .build();

        mockMvc.perform(post("/api/ai/generate-quiz")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Day 67: POST /api/ai/generate-quiz - Document with empty text returns HTTP 400")
    void testEmptyDocumentTextReturns400() throws Exception {
        document.setExtractedText("");
        documentRepository.save(document);

        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .documentId(document.getId())
                .questionCount(5)
                .build();

        mockMvc.perform(post("/api/ai/generate-quiz")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Day 67: POST /api/ai/generate-quiz - LLM failure returns clean HTTP 503 Service Unavailable")
    void testLlmFailureReturns503() throws Exception {
        when(chatClient.call(any(Prompt.class))).thenThrow(new RuntimeException("Connection refused by Ollama"));

        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .documentId(document.getId())
                .questionCount(5)
                .build();

        mockMvc.perform(post("/api/ai/generate-quiz")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Day 67: POST /api/ai/generate-quiz - Malformed AI JSON response returns clean HTTP 500")
    void testMalformedAiResponseReturns500() throws Exception {
        Generation generation = new Generation("This is not valid json at all {{{");
        ChatResponse mockResponse = new ChatResponse(java.util.List.of(generation));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .documentId(document.getId())
                .questionCount(5)
                .build();

        mockMvc.perform(post("/api/ai/generate-quiz")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Day 67: Draft AI Quiz can be published using existing /api/teacher/quizzes/{id}/publish")
    void testPublishGeneratedDraftQuiz() throws Exception {
        Generation generation = new Generation(getValidAiQuizJson());
        ChatResponse mockResponse = new ChatResponse(java.util.List.of(generation));
        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);

        GenerateQuizRequest request = GenerateQuizRequest.builder()
                .documentId(document.getId())
                .questionCount(2)
                .build();

        String responseStr = mockMvc.perform(post("/api/ai/generate-quiz")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var rootNode = objectMapper.readTree(responseStr);
        String quizIdStr = rootNode.path("data").path("quizId").asText();
        UUID quizId = UUID.fromString(quizIdStr);

        // Teacher reviews and publishes the draft quiz
        mockMvc.perform(post("/api/teacher/quizzes/" + quizId + "/publish")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.isPublished").value(true));

        Quiz publishedQuiz = quizRepository.findById(quizId).orElseThrow();
        assertEquals(QuizStatus.PUBLISHED, publishedQuiz.getStatus());
        assertTrue(publishedQuiz.getIsPublished());
    }

    @Test
    @DisplayName("Day 68: GET /api/ai/chat/history - Successfully retrieves persisted chat history")
    void testGetChatHistory() throws Exception {
        mockMvc.perform(get("/api/ai/chat/history")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray());
    }
}
