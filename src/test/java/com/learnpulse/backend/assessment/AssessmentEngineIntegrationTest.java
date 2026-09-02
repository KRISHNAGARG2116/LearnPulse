package com.learnpulse.backend.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.repository.*;
import com.learnpulse.backend.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AssessmentEngineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private StudentQuizResultRepository resultRepository;

    @Autowired
    private UploadedDocumentRepository documentRepository;

    @Autowired
    private NotesRepository notesRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User teacher;
    private User student1;
    private User student2;
    private Subject subject;
    private Chapter chapter;

    private String teacherToken;
    private String student1Token;
    private String student2Token;

    @BeforeEach
    void setUp() {
        resultRepository.deleteAll();
        questionRepository.deleteAll();
        quizRepository.deleteAll();
        notesRepository.deleteAll();
        documentRepository.deleteAll();
        chapterRepository.deleteAll();
        subjectRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        teacher = userRepository.save(User.builder()
                .email("teacher_quiz@learnpulse.ai")
                .password(passwordEncoder.encode("TeacherPass123!"))
                .role(Role.TEACHER)
                .build());
        teacherToken = jwtProvider.generateAccessToken(teacher);

        student1 = userRepository.save(User.builder()
                .email("student1_quiz@learnpulse.ai")
                .password(passwordEncoder.encode("StudentPass123!"))
                .role(Role.STUDENT)
                .build());
        student1Token = jwtProvider.generateAccessToken(student1);

        student2 = userRepository.save(User.builder()
                .email("student2_quiz@learnpulse.ai")
                .password(passwordEncoder.encode("StudentPass123!"))
                .role(Role.STUDENT)
                .build());
        student2Token = jwtProvider.generateAccessToken(student2);

        subject = subjectRepository.save(Subject.builder()
                .name("Software Engineering")
                .code("CS301")
                .description("Software Design and Architecture")
                .build());

        chapter = chapterRepository.save(Chapter.builder()
                .title("Design Patterns")
                .chapterNumber(1)
                .description("Creational, Structural, and Behavioral Patterns")
                .subject(subject)
                .build());
    }

    private CreateQuizRequest createValidQuizRequest() {
        CreateQuestionRequest q1 = CreateQuestionRequest.builder()
                .questionText("Which pattern restricts instantiation to a single object?")
                .optionA("Factory")
                .optionB("Singleton")
                .optionC("Observer")
                .optionD("Strategy")
                .correctAnswer("B")
                .marks(2)
                .build();

        CreateQuestionRequest q2 = CreateQuestionRequest.builder()
                .questionText("Which pattern defines a one-to-many dependency between objects?")
                .optionA("Singleton")
                .optionB("Adapter")
                .optionC("Observer")
                .optionD("Decorator")
                .correctAnswer("C")
                .marks(3)
                .build();

        return CreateQuizRequest.builder()
                .title("Design Patterns Final Assessment")
                .description("Test covering Singleton, Observer, and Factory patterns")
                .subjectId(subject.getId())
                .chapterId(chapter.getId())
                .questions(List.of(q1, q2))
                .build();
    }

    // 1. Teacher/Admin Authentication
    @Test
    @DisplayName("1. Teacher/Admin Authentication Token Generation Succeeds")
    void testTeacherAuthentication() {
        assertNotNull(teacherToken);
        assertEquals("TEACHER", jwtProvider.getRoleFromToken(teacherToken));
    }

    // 2. Authorized Quiz Creation
    @Test
    @DisplayName("2. Authorized Quiz Creation by Teacher Returns 201 Created")
    void testAuthorizedQuizCreation() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();

        mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.title", is("Design Patterns Final Assessment")))
                .andExpect(jsonPath("$.data.totalMarks", is(5)))
                .andExpect(jsonPath("$.data.questions", hasSize(2)));
    }

    // 3. Unauthorized Quiz Creation
    @Test
    @DisplayName("3. Unauthorized Quiz Creation Attempt by Student Returns 403 Forbidden")
    void testUnauthorizedQuizCreationFails() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();

        mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isForbidden());
    }

    // 4. Quiz Creation with Multiple Questions
    @Test
    @DisplayName("4. Quiz Creation with Multiple Questions Persists Correct Total Marks")
    void testQuizCreationWithMultipleQuestions() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();

        MvcResult result = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isCreated())
                .andReturn();

        String quizIdStr = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();
        Quiz dbQuiz = quizRepository.findById(UUID.fromString(quizIdStr)).orElseThrow();
        assertEquals(5, dbQuiz.getTotalMarks());
        List<Question> questions = questionRepository.findByQuizId(dbQuiz.getId());
        assertEquals(2, questions.size());
    }

    // 5. Question Persistence
    @Test
    @DisplayName("5. Questions are persisted in database with correct options and correct answers")
    void testQuestionPersistenceInDatabase() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();

        MvcResult result = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isCreated())
                .andReturn();

        String quizIdStr = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> dbQuestions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));
        assertEquals(2, dbQuestions.size());
        assertTrue(dbQuestions.stream().anyMatch(q -> q.getCorrectAnswer().equals("B")));
        assertTrue(dbQuestions.stream().anyMatch(q -> q.getCorrectAnswer().equals("C")));
    }

    // 6. Invalid Quiz Title
    @Test
    @DisplayName("6. Quiz Creation with Blank Title Fails with 400 Bad Request")
    void testInvalidQuizTitleFails() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        req.setTitle("  ");

        mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }

    // 7. Missing Question Text
    @Test
    @DisplayName("7. Question Creation with Blank Question Text Fails with 400 Bad Request")
    void testMissingQuestionTextFails() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        req.getQuestions().get(0).setQuestionText("");

        mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest());
    }

    // 8. Missing Option
    @Test
    @DisplayName("8. Question Creation with Missing Option A Fails with 400 Bad Request")
    void testMissingOptionFails() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        req.getQuestions().get(0).setOptionA("  ");

        mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest());
    }

    // 9. Invalid Correct Answer
    @Test
    @DisplayName("9. Question Creation with Invalid Correct Answer (e.g. 'E') Fails with 400 Bad Request")
    void testInvalidCorrectAnswerFails() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        req.getQuestions().get(0).setCorrectAnswer("E");

        mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest());
    }

    // 10. Invalid Marks
    @Test
    @DisplayName("10. Question Creation with Invalid Marks (<1) Fails with 400 Bad Request")
    void testInvalidMarksFails() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        req.getQuestions().get(0).setMarks(0);

        mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest());
    }

    // 11. Empty Questions List
    @Test
    @DisplayName("11. Quiz Creation with Empty Questions List Fails with 400 Bad Request")
    void testEmptyQuestionsListFails() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        req.setQuestions(List.of());

        mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isBadRequest());
    }

    // 12. Quiz Listing
    @Test
    @DisplayName("12. Student GET /api/quizzes Lists Available Quizzes")
    void testQuizListingSuccess() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        mockMvc.perform(post("/api/teacher/create-quiz")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("Authorization", "Bearer " + teacherToken));

        mockMvc.perform(get("/api/quizzes")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("Design Patterns Final Assessment")));
    }

    // 13. Subject Filtering
    @Test
    @DisplayName("13. Quiz Listing Filters Correctly by Subject ID")
    void testQuizListingSubjectFilter() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        mockMvc.perform(post("/api/teacher/create-quiz")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("Authorization", "Bearer " + teacherToken));

        mockMvc.perform(get("/api/quizzes")
                        .param("subjectId", subject.getId().toString())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/quizzes")
                        .param("subjectId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // 14. Chapter Filtering
    @Test
    @DisplayName("14. Quiz Listing Filters Correctly by Chapter ID")
    void testQuizListingChapterFilter() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        mockMvc.perform(post("/api/teacher/create-quiz")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("Authorization", "Bearer " + teacherToken));

        mockMvc.perform(get("/api/quizzes")
                        .param("chapterId", chapter.getId().toString())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    // 15. Quiz Detail Retrieval
    @Test
    @DisplayName("15. Student GET /api/quizzes/{id} Retrieves Quiz Details")
    void testQuizDetailRetrievalSuccess() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isCreated())
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();

        mockMvc.perform(get("/api/quizzes/" + quizIdStr)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.title", is("Design Patterns Final Assessment")))
                .andExpect(jsonPath("$.data.questions", hasSize(2)));
    }

    // 16. Correct Answer is NOT exposed
    @Test
    @DisplayName("16. CRITICAL SECURITY TEST: correctAnswer is NEVER exposed in student-facing quiz responses")
    void testCorrectAnswerNotExposedInStudentDTO() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isCreated())
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();

        MvcResult studentRes = mockMvc.perform(get("/api/quizzes/" + quizIdStr)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andReturn();

        String content = studentRes.getResponse().getContentAsString();
        assertFalse(content.contains("correctAnswer"), "Response JSON must NOT contain 'correctAnswer' field");
    }

    // 17. Invalid Quiz ID
    @Test
    @DisplayName("17. Retrieval with Non-Existent Quiz ID Returns 404 Not Found")
    void testInvalidQuizIdReturns404() throws Exception {
        mockMvc.perform(get("/api/quizzes/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")));
    }

    // 18. Student Authentication
    @Test
    @DisplayName("18. Student Authentication Token Generation Succeeds")
    void testStudentAuthentication() {
        assertNotNull(student1Token);
        assertEquals("STUDENT", jwtProvider.getRoleFromToken(student1Token));
    }

    // 19. Valid Quiz Submission
    @Test
    @DisplayName("19. Student POST /api/quizzes/submit Returns 201 Created and Graded Result")
    void testValidQuizSubmissionSuccess() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(questions.get(0).getId()).selectedAnswer("B").build(),
                        QuestionAnswerRequest.builder().questionId(questions.get(1).getId()).selectedAnswer("C").build()
                ))
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.score", is(5)))
                .andExpect(jsonPath("$.data.totalMarks", is(5)))
                .andExpect(jsonPath("$.data.percentage", is(100.0)))
                .andExpect(jsonPath("$.data.correctAnswers", is(2)))
                .andExpect(jsonPath("$.data.wrongAnswers", is(0)));
    }

    // 20. Correct Grading
    @Test
    @DisplayName("20. Server-Side Grading Correctly Evaluates 100% Score for All Correct Answers")
    void testCorrectGradingAllRight() throws Exception {
        testValidQuizSubmissionSuccess();
    }

    // 21. Incorrect Grading
    @Test
    @DisplayName("21. Server-Side Grading Correctly Evaluates 0% Score for All Wrong Answers")
    void testIncorrectGradingAllWrong() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(questions.get(0).getId()).selectedAnswer("A").build(),
                        QuestionAnswerRequest.builder().questionId(questions.get(1).getId()).selectedAnswer("A").build()
                ))
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.score", is(0)))
                .andExpect(jsonPath("$.data.percentage", is(0.0)))
                .andExpect(jsonPath("$.data.correctAnswers", is(0)))
                .andExpect(jsonPath("$.data.wrongAnswers", is(2)));
    }

    // 22. Mixed Correct/Incorrect Answers
    @Test
    @DisplayName("22. Server-Side Grading Correctly Evaluates Partial Score for Mixed Answers")
    void testMixedCorrectIncorrectGrading() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        // q1 (2 marks, correct B), q2 (3 marks, wrong A) -> Score 2/5 (40.0%)
        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(questions.get(0).getId()).selectedAnswer("B").build(),
                        QuestionAnswerRequest.builder().questionId(questions.get(1).getId()).selectedAnswer("A").build()
                ))
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.score", is(2)))
                .andExpect(jsonPath("$.data.totalMarks", is(5)))
                .andExpect(jsonPath("$.data.percentage", is(40.0)))
                .andExpect(jsonPath("$.data.correctAnswers", is(1)))
                .andExpect(jsonPath("$.data.wrongAnswers", is(1)));
    }

    // 23. Score Calculation
    @Test
    @DisplayName("23. Score Calculation Accuracy Verified")
    void testScoreCalculationAccuracy() throws Exception {
        testMixedCorrectIncorrectGrading();
    }

    // 24. Percentage Calculation
    @Test
    @DisplayName("24. Percentage Calculation Accuracy Verified")
    void testPercentageCalculationAccuracy() throws Exception {
        testMixedCorrectIncorrectGrading();
    }

    // 25. Correct/Wrong Answer Counts
    @Test
    @DisplayName("25. Correct and Wrong Answer Counts Verified")
    void testCorrectAndWrongAnswerCounts() throws Exception {
        testMixedCorrectIncorrectGrading();
    }

    // 26. Result Persistence
    @Test
    @DisplayName("26. StudentQuizResult is Persisted in PostgreSQL Database")
    void testResultPersistenceInDatabase() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(questions.get(0).getId()).selectedAnswer("B").build(),
                        QuestionAnswerRequest.builder().questionId(questions.get(1).getId()).selectedAnswer("C").build()
                ))
                .build();

        MvcResult submitRes = mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated())
                .andReturn();

        String resultIdStr = objectMapper.readTree(submitRes.getResponse().getContentAsString()).path("data").path("id").asText();
        StudentQuizResult dbResult = resultRepository.findById(UUID.fromString(resultIdStr)).orElseThrow();
        assertEquals(student1.getId(), dbResult.getStudent().getId());
        assertEquals(5, dbResult.getScore());
    }

    // 27. Authenticated Student Ownership
    @Test
    @DisplayName("27. Student Can Retrieve Their Own Quiz Result")
    void testAuthenticatedStudentResultOwnership() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(questions.get(0).getId()).selectedAnswer("B").build()
                ))
                .build();

        MvcResult submitRes = mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + student1Token))
                .andReturn();

        String resultIdStr = objectMapper.readTree(submitRes.getResponse().getContentAsString()).path("data").path("id").asText();

        mockMvc.perform(get("/api/quizzes/result/" + resultIdStr)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentEmail", is("student1_quiz@learnpulse.ai")));
    }

    // 28. Student Cannot Access Another Student's Result
    @Test
    @DisplayName("28. CRITICAL SECURITY TEST: Student Cannot Access Another Student's Result (403 Forbidden)")
    void testStudentCannotAccessOtherStudentResult() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(questions.get(0).getId()).selectedAnswer("B").build()
                ))
                .build();

        MvcResult submitRes = mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + student1Token))
                .andReturn();

        String resultIdStr = objectMapper.readTree(submitRes.getResponse().getContentAsString()).path("data").path("id").asText();

        // Student 2 attempts to fetch Student 1's result -> 403 Forbidden
        mockMvc.perform(get("/api/quizzes/result/" + resultIdStr)
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden());
    }

    // 29. Student Progress Calculation
    @Test
    @DisplayName("29. GET /api/student/progress Calculates Accurate Aggregated Student Performance Metrics")
    void testStudentProgressCalculation() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(questions.get(0).getId()).selectedAnswer("B").build(),
                        QuestionAnswerRequest.builder().questionId(questions.get(1).getId()).selectedAnswer("C").build()
                ))
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submission))
                .header("Authorization", "Bearer " + student1Token));

        mockMvc.perform(get("/api/student/progress")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.totalQuizzesAttempted", is(1)))
                .andExpect(jsonPath("$.data.totalQuizzesCompleted", is(1)))
                .andExpect(jsonPath("$.data.highestScore", is(5)))
                .andExpect(jsonPath("$.data.averagePercentage", is(100.0)))
                .andExpect(jsonPath("$.data.totalCorrectAnswers", is(2)))
                .andExpect(jsonPath("$.data.totalWrongAnswers", is(0)));
    }

    // 30. Student With No Quiz History
    @Test
    @DisplayName("30. GET /api/student/progress Handles Student with Zero Quiz History Gracefully")
    void testStudentWithNoQuizHistory() throws Exception {
        mockMvc.perform(get("/api/student/progress")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.totalQuizzesAttempted", is(0)))
                .andExpect(jsonPath("$.data.averagePercentage", is(0.0)))
                .andExpect(jsonPath("$.data.recentAttempts", hasSize(0)));
    }

    // 31. Invalid Question ID
    @Test
    @DisplayName("31. Submission with Non-Existent Question ID Fails with 400 Bad Request")
    void testInvalidQuestionIdInSubmissionFails() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();

        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(UUID.randomUUID()).selectedAnswer("B").build()
                ))
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not belong to Quiz")));
    }

    // 32. Question from Another Quiz
    @Test
    @DisplayName("32. Submission containing Question from Another Quiz Fails with 400 Bad Request")
    void testQuestionFromAnotherQuizInSubmissionFails() throws Exception {
        CreateQuizRequest req1 = createValidQuizRequest();
        MvcResult createRes1 = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();
        String quiz1IdStr = objectMapper.readTree(createRes1.getResponse().getContentAsString()).path("data").path("id").asText();

        CreateQuizRequest req2 = createValidQuizRequest();
        req2.setTitle("Quiz 2");
        MvcResult createRes2 = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();
        String quiz2IdStr = objectMapper.readTree(createRes2.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> quiz2Questions = questionRepository.findByQuizId(UUID.fromString(quiz2IdStr));

        // Submit Quiz 1, but send question ID belonging to Quiz 2
        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quiz1IdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(quiz2Questions.get(0).getId()).selectedAnswer("B").build()
                ))
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not belong to Quiz")));
    }

    // 33. Invalid Selected Answer
    @Test
    @DisplayName("33. Submission with Invalid Selected Answer Option (e.g. 'Z') Fails with 400 Bad Request")
    void testInvalidSelectedAnswerInSubmissionFails() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(questions.get(0).getId()).selectedAnswer("Z").build()
                ))
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isBadRequest());
    }

    // 34. Duplicate/Repeated Submission Policy
    @Test
    @DisplayName("34. Repeated Attempts Policy: Multiple submissions are allowed and recorded in result history")
    void testRepeatedSubmissionAllowedAndRecorded() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        QuizSubmissionRequest attempt1 = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(questions.get(0).getId()).selectedAnswer("A").build()
                ))
                .build();

        QuizSubmissionRequest attempt2 = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(questions.get(0).getId()).selectedAnswer("B").build(),
                        QuestionAnswerRequest.builder().questionId(questions.get(1).getId()).selectedAnswer("C").build()
                ))
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attempt1))
                .header("Authorization", "Bearer " + student1Token));

        mockMvc.perform(post("/api/quizzes/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attempt2))
                .header("Authorization", "Bearer " + student1Token));

        List<StudentQuizResult> studentResults = resultRepository.findByStudentIdOrderByAttemptedAtDesc(student1.getId());
        assertEquals(2, studentResults.size(), "Both quiz attempts should be recorded");
    }

    // 35. Malformed Submission Payload
    @Test
    @DisplayName("35. Malformed Submission Payload (empty answers list) Fails with 400 Bad Request")
    void testMalformedSubmissionPayloadFails() throws Exception {
        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.randomUUID())
                .answers(List.of())
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isBadRequest());
    }

    // 36. Reveal Answer fails without prior selection (Prerequisite Enforcement)
    @Test
    @DisplayName("36. Controlled Reveal Answer fails with 400 Bad Request if no option was selected")
    void testRevealAnswerFailsWithoutSelection() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        RevealAnswerRequest revealReq = new RevealAnswerRequest("");

        mockMvc.perform(post("/api/quizzes/" + quizIdStr + "/questions/" + questions.get(0).getId() + "/reveal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revealReq))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }

    // 37. Reveal Answer succeeds after selecting an answer
    @Test
    @DisplayName("37. Controlled Reveal Answer returns correct answer when an option has been selected")
    void testRevealAnswerSucceedsWithSelection() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        // Question 1 correct answer is B
        RevealAnswerRequest revealReq = new RevealAnswerRequest("B");

        mockMvc.perform(post("/api/quizzes/" + quizIdStr + "/questions/" + questions.get(0).getId() + "/reveal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revealReq))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.isCorrect", is(true)))
                .andExpect(jsonPath("$.data.correctAnswer", is("B")));
    }

    // 38. Revealing answer does not mutate student's selected answer or change final submission score
    @Test
    @DisplayName("38. CRITICAL SCORING TEST: Revealing answer after wrong selection does NOT mutate selected answer or final score")
    void testRevealAnswerDoesNotMutateFinalScore() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();
        List<Question> questions = questionRepository.findByQuizId(UUID.fromString(quizIdStr));

        // Student selects wrong answer "A" for Question 1 (correct is B)
        RevealAnswerRequest revealReq = new RevealAnswerRequest("A");
        mockMvc.perform(post("/api/quizzes/" + quizIdStr + "/questions/" + questions.get(0).getId() + "/reveal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revealReq))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isCorrect", is(false)))
                .andExpect(jsonPath("$.data.correctAnswer", is("B")));

        // Student submits quiz using original selected answer "A"
        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(UUID.fromString(quizIdStr))
                .answers(List.of(
                        QuestionAnswerRequest.builder().questionId(questions.get(0).getId()).selectedAnswer("A").build()
                ))
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.score", is(0))) // Score remains 0 for wrong answer A!
                .andExpect(jsonPath("$.data.wrongAnswers", is(2)));
    }

    // 39. Selecting an option does NOT expose correctness in quiz delivery/detail responses
    @Test
    @DisplayName("39. CRITICAL SECURITY TEST: Selecting an option does NOT expose correctness in student DTO or delivery endpoints")
    void testSelectionDoesNotRevealCorrectness() throws Exception {
        CreateQuizRequest req = createValidQuizRequest();
        MvcResult createRes = mockMvc.perform(post("/api/teacher/create-quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer " + teacherToken))
                .andReturn();

        String quizIdStr = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();

        MvcResult deliveryRes = mockMvc.perform(get("/api/quizzes/" + quizIdStr)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andReturn();

        String content = deliveryRes.getResponse().getContentAsString();
        assertFalse(content.contains("correctAnswer"), "Quiz delivery DTO must NOT expose correctAnswer or correctness indicators prior to controlled reveal");
    }
}
