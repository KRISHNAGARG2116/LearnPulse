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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CourseProgressionIntegrationTest {

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
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private StudentQuizResultRepository studentQuizResultRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User teacher;
    private User student;
    private String teacherToken;
    private String studentToken;
    private Subject subject;
    private Chapter chapter1;
    private Chapter chapter2;
    private Quiz ch1Quiz;
    private Quiz ch2Quiz;
    private Quiz finalQuiz;

    @BeforeEach
    void setUp() {
        studentQuizResultRepository.deleteAll();
        questionRepository.deleteAll();
        quizRepository.deleteAll();
        chapterRepository.deleteAll();
        subjectRepository.deleteAll();
        userRepository.deleteAll();

        teacher = User.builder()
                .email("teacher_prog@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.TEACHER)
                .build();
        teacher = userRepository.save(teacher);
        teacherToken = jwtProvider.generateAccessToken(teacher);

        student = User.builder()
                .email("student_prog@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.STUDENT)
                .build();
        student = userRepository.save(student);
        studentToken = jwtProvider.generateAccessToken(student);

        // Setup Course: Java Programming
        subject = Subject.builder()
                .name("Java Programming")
                .code("JAVA101")
                .description("Complete Java Course")
                .build();
        subject = subjectRepository.save(subject);

        chapter1 = Chapter.builder()
                .subject(subject)
                .chapterNumber(1)
                .title("Chapter 1: Basics")
                .description("Intro")
                .build();
        chapter1 = chapterRepository.save(chapter1);

        chapter2 = Chapter.builder()
                .subject(subject)
                .chapterNumber(2)
                .title("Chapter 2: OOP")
                .description("Object Oriented Programming")
                .build();
        chapter2 = chapterRepository.save(chapter2);

        // Chapter 1 Quiz (10 questions, 80% passing)
        ch1Quiz = createQuizWithQuestions("Chapter 1 Quiz", subject, chapter1, QuizType.CHAPTER_QUIZ, 10, 80.0, true);

        // Chapter 2 Quiz (10 questions, 80% passing)
        ch2Quiz = createQuizWithQuestions("Chapter 2 Quiz", subject, chapter2, QuizType.CHAPTER_QUIZ, 10, 80.0, true);

        // Final Course Quiz (25 questions, 75% passing)
        finalQuiz = createQuizWithQuestions("Final Course Assessment", subject, null, QuizType.FINAL_COURSE_QUIZ, 25, 75.0, true);
    }

    private Quiz createQuizWithQuestions(String title, Subject sub, Chapter ch, QuizType type, int questionCount, double passingScore, boolean isPublished) {
        List<Question> questions = new ArrayList<>();
        Quiz q = Quiz.builder()
                .title(title)
                .description("Quiz description for " + title)
                .subject(sub)
                .chapter(ch)
                .quizType(type)
                .status(isPublished ? QuizStatus.PUBLISHED : QuizStatus.DRAFT)
                .isPublished(isPublished)
                .passingScorePercentage(passingScore)
                .createdBy(teacher)
                .totalMarks(questionCount)
                .build();

        for (int i = 1; i <= questionCount; i++) {
            questions.add(Question.builder()
                    .quiz(q)
                    .questionText("Question " + i + " text")
                    .optionA("Option A")
                    .optionB("Option B")
                    .optionC("Option C")
                    .optionD("Option D")
                    .correctAnswer("A")
                    .source(QuestionSource.TEACHER_MANUAL)
                    .marks(1)
                    .build());
        }
        q.setQuestions(questions);
        return quizRepository.save(q);
    }

    @Test
    @DisplayName("1. Initial Course Progression: Chapter 1 unlocked, Chapter 2 & Final Quiz locked")
    void testInitialCourseProgression() throws Exception {
        mockMvc.perform(get("/api/student/courses/" + subject.getId() + "/progression")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.totalChapters", is(2)))
                .andExpect(jsonPath("$.data.completedChapters", is(0)))
                .andExpect(jsonPath("$.data.currentUnlockedChapterNumber", is(1)))
                .andExpect(jsonPath("$.data.isFinalQuizUnlocked", is(false)))
                .andExpect(jsonPath("$.data.isCourseCompleted", is(false)))
                .andExpect(jsonPath("$.data.chapters[0].isUnlocked", is(true)))
                .andExpect(jsonPath("$.data.chapters[1].isUnlocked", is(false)));
    }

    @Test
    @DisplayName("2. Direct API access to locked Chapter 2 Quiz returns 403 Forbidden")
    void testLockedChapterQuizAccessRejected() throws Exception {
        mockMvc.perform(get("/api/quizzes/" + ch2Quiz.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Prerequisite Chapter 1 quiz not passed")));
    }

    @Test
    @DisplayName("3. Failing Chapter 1 Quiz (< 80%) keeps Chapter 2 locked")
    void testFailingChapterQuizKeepsNextLocked() throws Exception {
        // Attempt Chapter 1 Quiz with 50% score (5 out of 10)
        List<QuestionAnswerRequest> answers = new ArrayList<>();
        List<Question> questions = ch1Quiz.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            // Give 5 correct (A), 5 wrong (B)
            String choice = i < 5 ? "A" : "B";
            answers.add(QuestionAnswerRequest.builder().questionId(questions.get(i).getId()).selectedAnswer(choice).build());
        }

        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(ch1Quiz.getId())
                .answers(answers)
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.percentage", is(50.0)))
                .andExpect(jsonPath("$.data.isPassed", is(false)));

        // Verify Chapter 2 Quiz is still locked
        mockMvc.perform(get("/api/quizzes/" + ch2Quiz.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Passing Chapter 1 Quiz (>= 80%) unlocks Chapter 2")
    void testPassingChapterQuizUnlocksNextChapter() throws Exception {
        // Attempt Chapter 1 Quiz with 90% score (9 out of 10)
        List<QuestionAnswerRequest> answers = new ArrayList<>();
        List<Question> questions = ch1Quiz.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            String choice = i < 9 ? "A" : "B";
            answers.add(QuestionAnswerRequest.builder().questionId(questions.get(i).getId()).selectedAnswer(choice).build());
        }

        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(ch1Quiz.getId())
                .answers(answers)
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.percentage", is(90.0)))
                .andExpect(jsonPath("$.data.isPassed", is(true)));

        // Verify Chapter 2 Quiz is NOW UNLOCKED and accessible
        mockMvc.perform(get("/api/quizzes/" + ch2Quiz.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(ch2Quiz.getId().toString())));
    }

    @Test
    @DisplayName("5. Passing all Chapter Quizzes unlocks Final Course Quiz")
    void testPassingAllChapterQuizzesUnlocksFinalQuiz() throws Exception {
        // Pass Ch 1 Quiz (100%)
        submitFullScoreQuiz(ch1Quiz.getId(), ch1Quiz.getQuestions());

        // Pass Ch 2 Quiz (100%)
        submitFullScoreQuiz(ch2Quiz.getId(), ch2Quiz.getQuestions());

        // Verify Progression shows Final Quiz unlocked
        mockMvc.perform(get("/api/student/courses/" + subject.getId() + "/progression")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedChapters", is(2)))
                .andExpect(jsonPath("$.data.isFinalQuizUnlocked", is(true)))
                .andExpect(jsonPath("$.data.isCourseCompleted", is(false)));

        // Verify Final Quiz is accessible
        mockMvc.perform(get("/api/quizzes/" + finalQuiz.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("6. Passing Final Course Quiz (>= 75%) marks Course Completed")
    void testPassingFinalQuizMarksCourseCompleted() throws Exception {
        // Pass Ch 1 & Ch 2
        submitFullScoreQuiz(ch1Quiz.getId(), ch1Quiz.getQuestions());
        submitFullScoreQuiz(ch2Quiz.getId(), ch2Quiz.getQuestions());

        // Attempt Final Quiz with 80% score (20 out of 25)
        List<QuestionAnswerRequest> answers = new ArrayList<>();
        List<Question> questions = finalQuiz.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            String choice = i < 20 ? "A" : "B";
            answers.add(QuestionAnswerRequest.builder().questionId(questions.get(i).getId()).selectedAnswer(choice).build());
        }

        QuizSubmissionRequest submission = QuizSubmissionRequest.builder()
                .quizId(finalQuiz.getId())
                .answers(answers)
                .build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.percentage", is(80.0)))
                .andExpect(jsonPath("$.data.isPassed", is(true)));

        // Verify Course is marked COMPLETED
        mockMvc.perform(get("/api/student/courses/" + subject.getId() + "/progression")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFinalQuizPassed", is(true)))
                .andExpect(jsonPath("$.data.isCourseCompleted", is(true)));
    }

    @Test
    @DisplayName("7. Draft Quiz is not exposed in student delivery list or detail API")
    void testDraftQuizHiddenFromStudents() throws Exception {
        Quiz draftQuiz = createQuizWithQuestions("Draft Quiz Title", subject, chapter1, QuizType.CHAPTER_QUIZ, 5, 80.0, false);

        // Student list API excludes draft quiz
        mockMvc.perform(get("/api/quizzes?chapterId=" + chapter1.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", not(hasItem(draftQuiz.getId().toString()))));

        // Direct access returns 403 Forbidden
        mockMvc.perform(get("/api/quizzes/" + draftQuiz.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("draft mode")));
    }

    @Test
    @DisplayName("8. Teacher can publish a Draft Quiz successfully")
    void testTeacherCanPublishDraftQuiz() throws Exception {
        Quiz draftQuiz = createQuizWithQuestions("Draft Quiz Title", subject, chapter1, QuizType.CHAPTER_QUIZ, 5, 80.0, false);

        mockMvc.perform(post("/api/teacher/quizzes/" + draftQuiz.getId() + "/publish")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.isPublished", is(true)))
                .andExpect(jsonPath("$.data.status", is("PUBLISHED")));
    }

    @Test
    @DisplayName("9. Multi-Chapter Progression: Passing 2 of 3 chapters keeps Final Quiz locked and rejects direct access")
    void testMultipleChaptersSomePassedFinalQuizRemainsLockedAndDirectAccessRejected() throws Exception {
        // Setup 3-chapter subject
        Subject multiSub = subjectRepository.save(Subject.builder().name("Python Data Science").code("PY301").description("3-Chapter Course").build());
        Chapter c1 = chapterRepository.save(Chapter.builder().subject(multiSub).chapterNumber(1).title("Numpy").build());
        Chapter c2 = chapterRepository.save(Chapter.builder().subject(multiSub).chapterNumber(2).title("Pandas").build());
        Chapter c3 = chapterRepository.save(Chapter.builder().subject(multiSub).chapterNumber(3).title("Matplotlib").build());

        Quiz q1 = createQuizWithQuestions("Numpy Quiz", multiSub, c1, QuizType.CHAPTER_QUIZ, 10, 80.0, true);
        Quiz q2 = createQuizWithQuestions("Pandas Quiz", multiSub, c2, QuizType.CHAPTER_QUIZ, 10, 80.0, true);
        Quiz q3 = createQuizWithQuestions("Matplotlib Quiz", multiSub, c3, QuizType.CHAPTER_QUIZ, 10, 80.0, true);
        Quiz fQuiz = createQuizWithQuestions("Final Python Assessment", multiSub, null, QuizType.FINAL_COURSE_QUIZ, 25, 75.0, true);

        // Pass Chapter 1 & Chapter 2, leave Chapter 3 unattempted
        submitFullScoreQuiz(q1.getId(), q1.getQuestions());
        submitFullScoreQuiz(q2.getId(), q2.getQuestions());

        // Verify Progression API: 2/3 completed, Final Quiz LOCKED
        mockMvc.perform(get("/api/student/courses/" + multiSub.getId() + "/progression")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalChapters", is(3)))
                .andExpect(jsonPath("$.data.completedChapters", is(2)))
                .andExpect(jsonPath("$.data.isFinalQuizUnlocked", is(false)));

        // Direct GET API access to Final Quiz must be rejected (403 Forbidden)
        mockMvc.perform(get("/api/quizzes/" + fQuiz.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Final Course Quiz is locked")));

        // Direct POST submission API access to Final Quiz must be rejected (403 Forbidden)
        List<QuestionAnswerRequest> answers = new ArrayList<>();
        for (Question q : fQuiz.getQuestions()) {
            answers.add(QuestionAnswerRequest.builder().questionId(q.getId()).selectedAnswer("A").build());
        }
        QuizSubmissionRequest submission = QuizSubmissionRequest.builder().quizId(fQuiz.getId()).answers(answers).build();
        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Final Course Quiz is locked")));
    }

    @Test
    @DisplayName("10. Multi-Chapter Progression: Passing ALL 3 chapters unlocks Final Quiz")
    void testMultipleChaptersAllPassedUnlocksFinalQuiz() throws Exception {
        Subject multiSub = subjectRepository.save(Subject.builder().name("Machine Learning").code("ML401").description("3-Chapter ML Course").build());
        Chapter c1 = chapterRepository.save(Chapter.builder().subject(multiSub).chapterNumber(1).title("Linear Models").build());
        Chapter c2 = chapterRepository.save(Chapter.builder().subject(multiSub).chapterNumber(2).title("Tree Models").build());
        Chapter c3 = chapterRepository.save(Chapter.builder().subject(multiSub).chapterNumber(3).title("Neural Networks").build());

        Quiz q1 = createQuizWithQuestions("Linear Quiz", multiSub, c1, QuizType.CHAPTER_QUIZ, 10, 80.0, true);
        Quiz q2 = createQuizWithQuestions("Tree Quiz", multiSub, c2, QuizType.CHAPTER_QUIZ, 10, 80.0, true);
        Quiz q3 = createQuizWithQuestions("Neural Quiz", multiSub, c3, QuizType.CHAPTER_QUIZ, 10, 80.0, true);
        Quiz fQuiz = createQuizWithQuestions("Final ML Assessment", multiSub, null, QuizType.FINAL_COURSE_QUIZ, 25, 75.0, true);

        // Pass ALL 3 chapter quizzes
        submitFullScoreQuiz(q1.getId(), q1.getQuestions());
        submitFullScoreQuiz(q2.getId(), q2.getQuestions());
        submitFullScoreQuiz(q3.getId(), q3.getQuestions());

        // Verify Progression API: 3/3 completed, Final Quiz UNLOCKED
        mockMvc.perform(get("/api/student/courses/" + multiSub.getId() + "/progression")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalChapters", is(3)))
                .andExpect(jsonPath("$.data.completedChapters", is(3)))
                .andExpect(jsonPath("$.data.isFinalQuizUnlocked", is(true)));

        // Direct GET API access to Final Quiz must succeed (200 OK)
        mockMvc.perform(get("/api/quizzes/" + fQuiz.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(fQuiz.getId().toString())));
    }

    private void submitFullScoreQuiz(UUID qId, List<Question> questions) throws Exception {
        List<QuestionAnswerRequest> answers = new ArrayList<>();
        for (Question q : questions) {
            answers.add(QuestionAnswerRequest.builder().questionId(q.getId()).selectedAnswer(q.getCorrectAnswer()).build());
        }
        QuizSubmissionRequest submission = QuizSubmissionRequest.builder().quizId(qId).answers(answers).build();
        mockMvc.perform(post("/api/quizzes/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission))
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated());
    }
}
