package com.learnpulse.backend.enrollment;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EnrollmentUserJourneyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private TeacherSubjectRepository teacherSubjectRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

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

    private User admin;
    private User teacher;
    private User student1;
    private User student2;

    private String adminToken;
    private String teacherToken;
    private String student1Token;
    private String student2Token;

    private Subject course;
    private Chapter chapter1;
    private Quiz quiz1;

    @BeforeEach
    void setUp() {
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

        // 1. Admin
        admin = userRepository.save(User.builder()
                .email("admin_journey@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.ADMIN)
                .build());
        adminToken = jwtProvider.generateAccessToken(admin);

        // 2. Teacher
        teacher = userRepository.save(User.builder()
                .email("prof_alan@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.TEACHER)
                .build());
        teacherToken = jwtProvider.generateAccessToken(teacher);
        userProfileRepository.save(UserProfile.builder()
                .user(teacher)
                .firstName("Alan")
                .lastName("Turing")
                .department("Computer Science")
                .build());

        // 3. Students
        student1 = userRepository.save(User.builder()
                .email("student1_journey@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.STUDENT)
                .build());
        student1Token = jwtProvider.generateAccessToken(student1);
        userProfileRepository.save(UserProfile.builder()
                .user(student1)
                .firstName("Grace")
                .lastName("Hopper")
                .department("Computer Science")
                .enrollmentNumber("STU-1906")
                .build());

        student2 = userRepository.save(User.builder()
                .email("student2_journey@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.STUDENT)
                .build());
        student2Token = jwtProvider.generateAccessToken(student2);
        userProfileRepository.save(UserProfile.builder()
                .user(student2)
                .firstName("Ada")
                .lastName("Lovelace")
                .department("Mathematics")
                .enrollmentNumber("STU-1815")
                .build());

        // 4. Academic Hierarchy
        course = subjectRepository.save(Subject.builder()
                .name("Theory of Computation")
                .code("CS401")
                .description("Automata, Languages, and Complexity")
                .build());

        chapter1 = chapterRepository.save(Chapter.builder()
                .subject(course)
                .chapterNumber(1)
                .title("Finite Automata")
                .description("Deterministic and Nondeterministic Finite Automata")
                .build());

        // Chapter 1 Quiz (1 question)
        List<Question> questions = new ArrayList<>();
        quiz1 = Quiz.builder()
                .title("DFA Quiz")
                .subject(course)
                .chapter(chapter1)
                .quizType(QuizType.CHAPTER_QUIZ)
                .status(QuizStatus.PUBLISHED)
                .isPublished(true)
                .createdBy(teacher)
                .totalMarks(1)
                .passingScorePercentage(80.0)
                .build();

        Question q1 = Question.builder()
                .quiz(quiz1)
                .questionText("A DFA has how many transitions per state for each symbol?")
                .optionA("Exactly one")
                .optionB("Zero or more")
                .optionC("At least two")
                .optionD("None")
                .correctAnswer("A")
                .marks(1)
                .source(QuestionSource.TEACHER_MANUAL)
                .build();
        questions.add(q1);
        quiz1.setQuestions(questions);
        quiz1 = quizRepository.save(quiz1);
    }

    @Test
    @DisplayName("Complete Multi-Persona Journey: Admin assigns teacher -> Teacher enrolls student -> Student learns and passes quiz -> Teacher unenrolls -> Historical results preserved -> Re-enrollment reactivates")
    void testCompleteEnrollmentUserJourney() throws Exception {

        // ==========================================
        // STEP 1: Admin assigns Professor Alan to Theory of Computation
        // ==========================================
        AssignTeacherRequest assignReq = AssignTeacherRequest.builder()
                .teacherId(teacher.getId())
                .build();

        mockMvc.perform(post("/api/admin/subjects/" + course.getId() + "/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.subjectId", is(course.getId().toString())));

        // ==========================================
        // STEP 2: Professor Alan logs in and verifies Theory of Computation appears in his assigned subjects
        // ==========================================
        mockMvc.perform(get("/api/teacher/enrollment/subjects")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].subjectId", is(course.getId().toString())))
                .andExpect(jsonPath("$.data[0].subjectName", is("Theory of Computation")));

        // ==========================================
        // STEP 3: Professor Alan enrolls Grace Hopper into Theory of Computation
        // ==========================================
        EnrollStudentRequest enrollReq = EnrollStudentRequest.builder()
                .studentId(student1.getId())
                .build();

        mockMvc.perform(post("/api/teacher/enrollment/subjects/" + course.getId() + "/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentEmail", is(student1.getEmail())))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        // ==========================================
        // STEP 4: Grace Hopper logs in, sees Theory of Computation with Prof. Alan Turing listed
        // ==========================================
        mockMvc.perform(get("/api/student/enrollment/subjects")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].subjectName", is("Theory of Computation")))
                .andExpect(jsonPath("$.data[0].teacherFirstName", is("Alan")))
                .andExpect(jsonPath("$.data[0].teacherLastName", is("Turing")));

        // ==========================================
        // STEP 5: Grace Hopper accesses chapters, views quiz, and completes progression
        // ==========================================
        mockMvc.perform(get("/api/subjects/" + course.getId() + "/chapters")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("Finite Automata")));

        mockMvc.perform(get("/api/quizzes/" + quiz1.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("DFA Quiz")));

        // Grace submits quiz and scores 100%
        List<QuestionAnswerRequest> answers = List.of(
                QuestionAnswerRequest.builder().questionId(quiz1.getQuestions().get(0).getId()).selectedAnswer("A").build()
        );
        QuizSubmissionRequest submission = QuizSubmissionRequest.builder().quizId(quiz1.getId()).answers(answers).build();

        mockMvc.perform(post("/api/quizzes/submit")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.score", is(1)))
                .andExpect(jsonPath("$.data.percentage", is(100.0)))
                .andExpect(jsonPath("$.data.isPassed", is(true)));

        // ==========================================
        // STEP 6: Ada Lovelace (unenrolled student) is denied access
        // ==========================================
        mockMvc.perform(get("/api/subjects/" + course.getId() + "/chapters")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not actively enrolled")));

        mockMvc.perform(get("/api/quizzes/" + quiz1.getId())
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/student/courses/" + course.getId() + "/progression")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden());

        // ==========================================
        // STEP 7: Professor Alan unenrolls Grace Hopper (soft-delete to INACTIVE)
        // ==========================================
        mockMvc.perform(delete("/api/teacher/enrollment/subjects/" + course.getId() + "/students/" + student1.getId())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")));

        // ==========================================
        // STEP 8: Historical results preserved and access revoked
        // ==========================================
        // Grace's quiz results are STILL preserved in database
        assertEquals(1, resultRepository.count());
        StudentQuizResult preserved = resultRepository.findAll().get(0);
        assertEquals(student1.getId(), preserved.getStudent().getId());
        assertEquals(100.0, preserved.getPercentage());

        // Grace is now denied access to chapters and quiz
        mockMvc.perform(get("/api/subjects/" + course.getId() + "/chapters")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not actively enrolled")));

        mockMvc.perform(get("/api/quizzes/" + quiz1.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isForbidden());

        // ==========================================
        // STEP 9: Professor Alan re-enrolls Grace Hopper (reactivates record to ACTIVE)
        // ==========================================
        mockMvc.perform(post("/api/teacher/enrollment/subjects/" + course.getId() + "/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        // Only 1 enrollment record exists in database (reactivated, not duplicated)
        assertEquals(1, studentEnrollmentRepository.count());

        // Grace can access again with previous score intact
        mockMvc.perform(get("/api/subjects/" + course.getId() + "/chapters")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/student/courses/" + course.getId() + "/progression")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedChapters", is(1)))
                .andExpect(jsonPath("$.data.chapters[0].highestScorePercentage", is(100.0)));
    }
}
