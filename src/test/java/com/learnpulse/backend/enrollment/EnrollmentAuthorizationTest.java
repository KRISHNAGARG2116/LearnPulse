package com.learnpulse.backend.enrollment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.AssignTeacherRequest;
import com.learnpulse.backend.dto.EnrollStudentRequest;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EnrollmentAuthorizationTest {

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
    private User teacherA;
    private User teacherB;
    private User student1;
    private User student2;

    private String adminToken;
    private String teacherAToken;
    private String teacherBToken;
    private String student1Token;
    private String student2Token;

    private Subject subjectA;
    private Subject subjectB;
    private Chapter chapterA;
    private Quiz quizA;
    private UploadedDocument docA;

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

        // 1. Create Users
        admin = userRepository.save(User.builder()
                .email("admin_auth@learnpulse.ai")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ADMIN)
                .build());
        adminToken = jwtProvider.generateAccessToken(admin);

        teacherA = userRepository.save(User.builder()
                .email("teacherA_auth@learnpulse.ai")
                .password(passwordEncoder.encode("TeacherPass123!"))
                .role(Role.TEACHER)
                .build());
        teacherAToken = jwtProvider.generateAccessToken(teacherA);

        teacherB = userRepository.save(User.builder()
                .email("teacherB_auth@learnpulse.ai")
                .password(passwordEncoder.encode("TeacherPass123!"))
                .role(Role.TEACHER)
                .build());
        teacherBToken = jwtProvider.generateAccessToken(teacherB);

        student1 = userRepository.save(User.builder()
                .email("student1_auth@learnpulse.ai")
                .password(passwordEncoder.encode("StudentPass123!"))
                .role(Role.STUDENT)
                .build());
        student1Token = jwtProvider.generateAccessToken(student1);

        student2 = userRepository.save(User.builder()
                .email("student2_auth@learnpulse.ai")
                .password(passwordEncoder.encode("StudentPass123!"))
                .role(Role.STUDENT)
                .build());
        student2Token = jwtProvider.generateAccessToken(student2);

        // 2. Create Subjects
        subjectA = subjectRepository.save(Subject.builder()
                .name("Computer Networks")
                .code("NET401")
                .description("Networking fundamentals")
                .build());

        subjectB = subjectRepository.save(Subject.builder()
                .name("Cloud Computing")
                .code("CLOUD501")
                .description("Cloud infrastructure")
                .build());

        // Assign Teacher A to Subject A only
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(teacherA)
                .subject(subjectA)
                .build());

        // Create Chapter and Quiz for Subject A
        chapterA = chapterRepository.save(Chapter.builder()
                .subject(subjectA)
                .chapterNumber(1)
                .title("OSI Model")
                .build());

        quizA = quizRepository.save(Quiz.builder()
                .title("OSI Model Quiz")
                .subject(subjectA)
                .chapter(chapterA)
                .quizType(QuizType.CHAPTER_QUIZ)
                .status(QuizStatus.PUBLISHED)
                .isPublished(true)
                .createdBy(teacherA)
                .totalMarks(1)
                .passingScorePercentage(80.0)
                .build());

        docA = documentRepository.save(UploadedDocument.builder()
                .originalFileName("osi_reference.pdf")
                .storedFileName("stored_osi_reference.pdf")
                .filePath("/tmp/stored_osi_reference.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .teacher(teacherA)
                .subject(subjectA)
                .chapter(chapterA)
                .active(true)
                .processingStatus(ProcessingStatus.PROCESSED)
                .extractedText("OSI Model 7 layers...")
                .build());
    }

    // ==========================================
    // 1-3: TEACHER OWNERSHIP BOUNDARY TESTS
    // ==========================================

    @Test
    @DisplayName("1. Teacher cannot enroll student in an unassigned subject (403 Forbidden)")
    void testTeacherCannotEnrollInUnassignedSubject() throws Exception {
        // Teacher B is NOT assigned to Subject A
        EnrollStudentRequest req = EnrollStudentRequest.builder()
                .studentId(student1.getId())
                .build();

        mockMvc.perform(post("/api/teacher/enrollment/subjects/" + subjectA.getId() + "/students")
                        .header("Authorization", "Bearer " + teacherBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not assigned to manage enrollments")));
    }

    @Test
    @DisplayName("2. Teacher cannot view enrolled students of an unassigned subject (403 Forbidden)")
    void testTeacherCannotViewStudentsOfUnassignedSubject() throws Exception {
        // Teacher B attempts to view Subject A students
        mockMvc.perform(get("/api/teacher/enrollment/subjects/" + subjectA.getId() + "/students")
                        .header("Authorization", "Bearer " + teacherBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not assigned to view students")));
    }

    @Test
    @DisplayName("3. Teacher cannot unenroll a student from an unassigned subject (403 Forbidden)")
    void testTeacherCannotUnenrollFromUnassignedSubject() throws Exception {
        // Enroll student1 in subjectA
        studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student1)
                .subject(subjectA)
                .status(EnrollmentStatus.ACTIVE)
                .build());

        // Teacher B attempts to unenroll student1 from Subject A
        mockMvc.perform(delete("/api/teacher/enrollment/subjects/" + subjectA.getId() + "/students/" + student1.getId())
                        .header("Authorization", "Bearer " + teacherBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not assigned to manage enrollments")));
    }

    // ==========================================
    // 4-8: UNENROLLED STUDENT ACCESS GATING
    // ==========================================

    @Test
    @DisplayName("4. Unenrolled student cannot access subject chapters (403 Forbidden)")
    void testUnenrolledStudentCannotAccessChapters() throws Exception {
        // student2 is not enrolled in subjectA
        mockMvc.perform(get("/api/subjects/" + subjectA.getId() + "/chapters")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not actively enrolled in this subject")));
    }

    @Test
    @DisplayName("5. Unenrolled student cannot access subject quizzes (403 Forbidden)")
    void testUnenrolledStudentCannotAccessQuizzes() throws Exception {
        // student2 is not enrolled in subjectA
        mockMvc.perform(get("/api/quizzes")
                        .param("subjectId", subjectA.getId().toString())
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not actively enrolled in this subject")));
    }

    @Test
    @DisplayName("6. Unenrolled student cannot access individual quiz details (403 Forbidden)")
    void testUnenrolledStudentCannotAccessIndividualQuiz() throws Exception {
        // student2 is not enrolled in subjectA
        mockMvc.perform(get("/api/quizzes/" + quizA.getId())
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not actively enrolled in the subject for this quiz")));
    }

    @Test
    @DisplayName("7. Unenrolled student cannot access course progression (403 Forbidden)")
    void testUnenrolledStudentCannotAccessProgression() throws Exception {
        // student2 is not enrolled in subjectA
        mockMvc.perform(get("/api/student/courses/" + subjectA.getId() + "/progression")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not actively enrolled in this course")));
    }

    @Test
    @DisplayName("8. Unenrolled student cannot download subject document (403 Forbidden)")
    void testUnenrolledStudentCannotDownloadDocument() throws Exception {
        // student2 is not enrolled in subjectA
        mockMvc.perform(get("/api/documents/" + docA.getId() + "/download")
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not actively enrolled in the subject of this document")));
    }

    // ==========================================
    // 9-11: INACTIVE STUDENT ACCESS GATING
    // ==========================================

    @Test
    @DisplayName("9. Inactive (unenrolled) student cannot access chapters (403 Forbidden)")
    void testInactiveStudentCannotAccessChapters() throws Exception {
        // student1 has INACTIVE status
        studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student1)
                .subject(subjectA)
                .status(EnrollmentStatus.INACTIVE)
                .build());

        mockMvc.perform(get("/api/subjects/" + subjectA.getId() + "/chapters")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not actively enrolled in this subject")));
    }

    @Test
    @DisplayName("10. Inactive (unenrolled) student cannot access quiz (403 Forbidden)")
    void testInactiveStudentCannotAccessQuiz() throws Exception {
        studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student1)
                .subject(subjectA)
                .status(EnrollmentStatus.INACTIVE)
                .build());

        mockMvc.perform(get("/api/quizzes/" + quizA.getId())
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not actively enrolled in the subject for this quiz")));
    }

    @Test
    @DisplayName("11. Inactive (unenrolled) student does not see subject in enrolled subjects")
    void testInactiveStudentExcludedFromEnrolledList() throws Exception {
        studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student1)
                .subject(subjectA)
                .status(EnrollmentStatus.INACTIVE)
                .build());

        mockMvc.perform(get("/api/student/enrollment/subjects")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ==========================================
    // 12-17: RBAC ROLE CROSS-ACCESS TESTS
    // ==========================================

    @Test
    @DisplayName("12. Student cannot access teacher enrollment endpoints (403 Forbidden)")
    void testStudentCannotAccessTeacherEnrollmentEndpoints() throws Exception {
        mockMvc.perform(get("/api/teacher/enrollment/subjects")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("13. Student cannot access admin teacher assignment endpoints (403 Forbidden)")
    void testStudentCannotAccessAdminEndpoints() throws Exception {
        AssignTeacherRequest req = AssignTeacherRequest.builder().teacherId(teacherA.getId()).build();

        mockMvc.perform(post("/api/admin/subjects/" + subjectA.getId() + "/teachers")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14. Teacher cannot access admin teacher assignment endpoints (403 Forbidden)")
    void testTeacherCannotAccessAdminEndpoints() throws Exception {
        AssignTeacherRequest req = AssignTeacherRequest.builder().teacherId(teacherB.getId()).build();

        mockMvc.perform(post("/api/admin/subjects/" + subjectA.getId() + "/teachers")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15. Unauthenticated request to teacher enrollment is rejected (401 Unauthorized)")
    void testUnauthenticatedTeacherEnrollmentRejected() throws Exception {
        mockMvc.perform(get("/api/teacher/enrollment/subjects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("16. Unauthenticated request to student enrollment is rejected (401 Unauthorized)")
    void testUnauthenticatedStudentEnrollmentRejected() throws Exception {
        mockMvc.perform(get("/api/student/enrollment/subjects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("17. Teacher cannot enroll a non-student user (400 Bad Request)")
    void testTeacherCannotEnrollNonStudent() throws Exception {
        EnrollStudentRequest req = EnrollStudentRequest.builder()
                .studentId(teacherB.getId()) // Passing teacher ID instead of student ID
                .build();

        mockMvc.perform(post("/api/teacher/enrollment/subjects/" + subjectA.getId() + "/students")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Only STUDENTS can be enrolled")));
    }
}
