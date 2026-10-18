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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EnrollmentIntegrationTest {

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
    private User teacher1;
    private User teacher2;
    private User student1;
    private User student2;

    private String adminToken;
    private String teacher1Token;
    private String teacher2Token;
    private String student1Token;
    private String student2Token;

    private Subject subject1;
    private Subject subject2;

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

        // 1. Create Admin
        admin = userRepository.save(User.builder()
                .email("admin_enr@learnpulse.ai")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ADMIN)
                .build());
        adminToken = jwtProvider.generateAccessToken(admin);

        // 2. Create Teachers
        teacher1 = userRepository.save(User.builder()
                .email("teacher1_enr@learnpulse.ai")
                .password(passwordEncoder.encode("TeacherPass123!"))
                .role(Role.TEACHER)
                .build());
        teacher1Token = jwtProvider.generateAccessToken(teacher1);
        userProfileRepository.save(UserProfile.builder()
                .user(teacher1)
                .firstName("Alice")
                .lastName("Smith")
                .department("Computer Science")
                .build());

        teacher2 = userRepository.save(User.builder()
                .email("teacher2_enr@learnpulse.ai")
                .password(passwordEncoder.encode("TeacherPass123!"))
                .role(Role.TEACHER)
                .build());
        teacher2Token = jwtProvider.generateAccessToken(teacher2);

        // 3. Create Students
        student1 = userRepository.save(User.builder()
                .email("student1_enr@learnpulse.ai")
                .password(passwordEncoder.encode("StudentPass123!"))
                .role(Role.STUDENT)
                .build());
        student1Token = jwtProvider.generateAccessToken(student1);
        userProfileRepository.save(UserProfile.builder()
                .user(student1)
                .firstName("Bob")
                .lastName("Jones")
                .department("Information Technology")
                .enrollmentNumber("STU-1001")
                .build());

        student2 = userRepository.save(User.builder()
                .email("student2_enr@learnpulse.ai")
                .password(passwordEncoder.encode("StudentPass123!"))
                .role(Role.STUDENT)
                .build());
        student2Token = jwtProvider.generateAccessToken(student2);
        userProfileRepository.save(UserProfile.builder()
                .user(student2)
                .firstName("Charlie")
                .lastName("Brown")
                .department("Computer Science")
                .enrollmentNumber("STU-1002")
                .build());

        // 4. Create Subjects
        subject1 = subjectRepository.save(Subject.builder()
                .name("Data Structures")
                .code("CS201")
                .description("Core Data Structures")
                .build());

        subject2 = subjectRepository.save(Subject.builder()
                .name("Operating Systems")
                .code("CS301")
                .description("Processes and Memory Management")
                .build());

        chapterRepository.save(Chapter.builder()
                .subject(subject1)
                .chapterNumber(1)
                .title("Arrays & Lists")
                .build());
    }

    // ==========================================
    // ADMIN TEACHER-SUBJECT TESTS
    // ==========================================

    @Test
    @DisplayName("Admin assigns teacher to subject succeeds (200 OK)")
    void testAdminAssignTeacherSuccess() throws Exception {
        AssignTeacherRequest req = AssignTeacherRequest.builder()
                .teacherId(teacher1.getId())
                .build();

        mockMvc.perform(post("/api/admin/subjects/" + subject1.getId() + "/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.subjectId", is(subject1.getId().toString())))
                .andExpect(jsonPath("$.data.subjectName", is("Data Structures")))
                .andExpect(jsonPath("$.data.chapterCount", is(1)));

        assertTrue(teacherSubjectRepository.existsByTeacherIdAndSubjectId(teacher1.getId(), subject1.getId()));
    }

    @Test
    @DisplayName("Admin assigning same teacher twice returns 409 Conflict")
    void testAdminAssignDuplicateTeacherReturnsConflict() throws Exception {
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(teacher1)
                .subject(subject1)
                .build());

        AssignTeacherRequest req = AssignTeacherRequest.builder()
                .teacherId(teacher1.getId())
                .build();

        mockMvc.perform(post("/api/admin/subjects/" + subject1.getId() + "/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already assigned")));
    }

    @Test
    @DisplayName("Admin assigning non-teacher role returns 400 Bad Request")
    void testAdminAssignStudentAsTeacherReturnsBadRequest() throws Exception {
        AssignTeacherRequest req = AssignTeacherRequest.builder()
                .teacherId(student1.getId())
                .build();

        mockMvc.perform(post("/api/admin/subjects/" + subject1.getId() + "/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("is not a teacher")));
    }

    @Test
    @DisplayName("Admin removes teacher assignment succeeds (200 OK)")
    void testAdminRemoveTeacherSuccess() throws Exception {
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(teacher1)
                .subject(subject1)
                .build());

        mockMvc.perform(delete("/api/admin/subjects/" + subject1.getId() + "/teachers/" + teacher1.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")));

        assertFalse(teacherSubjectRepository.existsByTeacherIdAndSubjectId(teacher1.getId(), subject1.getId()));
    }

    @Test
    @DisplayName("List teachers for subject returns assigned teachers")
    void testGetSubjectTeachers() throws Exception {
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(teacher1)
                .subject(subject1)
                .build());

        mockMvc.perform(get("/api/admin/subjects/" + subject1.getId() + "/teachers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].email", is(teacher1.getEmail())))
                .andExpect(jsonPath("$.data[0].firstName", is("Alice")));
    }

    // ==========================================
    // TEACHER ENROLLMENT MANAGEMENT TESTS
    // ==========================================

    @Test
    @DisplayName("Teacher views assigned subjects returns only their subjects")
    void testTeacherViewsAssignedSubjects() throws Exception {
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(teacher1)
                .subject(subject1)
                .build());
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(teacher2)
                .subject(subject2)
                .build());

        mockMvc.perform(get("/api/teacher/enrollment/subjects")
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].subjectId", is(subject1.getId().toString())))
                .andExpect(jsonPath("$.data[0].subjectName", is("Data Structures")));
    }

    @Test
    @DisplayName("Teacher enrolls student in assigned subject succeeds (201 Created)")
    void testTeacherEnrollsStudentSuccess() throws Exception {
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(teacher1)
                .subject(subject1)
                .build());

        EnrollStudentRequest req = EnrollStudentRequest.builder()
                .studentId(student1.getId())
                .build();

        mockMvc.perform(post("/api/teacher/enrollment/subjects/" + subject1.getId() + "/students")
                        .header("Authorization", "Bearer " + teacher1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.studentEmail", is(student1.getEmail())))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        assertTrue(studentEnrollmentRepository.existsByStudentIdAndSubjectIdAndStatus(student1.getId(), subject1.getId(), EnrollmentStatus.ACTIVE));
    }

    @Test
    @DisplayName("Teacher enrolling already enrolled student returns 409 Conflict")
    void testTeacherEnrollDuplicateReturnsConflict() throws Exception {
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(teacher1)
                .subject(subject1)
                .build());

        studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student1)
                .subject(subject1)
                .status(EnrollmentStatus.ACTIVE)
                .build());

        EnrollStudentRequest req = EnrollStudentRequest.builder()
                .studentId(student1.getId())
                .build();

        mockMvc.perform(post("/api/teacher/enrollment/subjects/" + subject1.getId() + "/students")
                        .header("Authorization", "Bearer " + teacher1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already actively enrolled")));
    }

    @Test
    @DisplayName("Teacher unenrolls student soft-deletes to INACTIVE (200 OK)")
    void testTeacherUnenrollsStudentSoftDelete() throws Exception {
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(teacher1)
                .subject(subject1)
                .build());

        StudentEnrollment enr = studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student1)
                .subject(subject1)
                .status(EnrollmentStatus.ACTIVE)
                .build());

        mockMvc.perform(delete("/api/teacher/enrollment/subjects/" + subject1.getId() + "/students/" + student1.getId())
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")));

        StudentEnrollment updated = studentEnrollmentRepository.findById(enr.getId()).orElseThrow();
        assertEquals(EnrollmentStatus.INACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Teacher re-enrolling inactive student reactivates record (201/200 OK)")
    void testTeacherReenrollReactivatesRecord() throws Exception {
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(teacher1)
                .subject(subject1)
                .build());

        StudentEnrollment enr = studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student1)
                .subject(subject1)
                .status(EnrollmentStatus.INACTIVE)
                .build());

        EnrollStudentRequest req = EnrollStudentRequest.builder()
                .studentId(student1.getId())
                .build();

        mockMvc.perform(post("/api/teacher/enrollment/subjects/" + subject1.getId() + "/students")
                        .header("Authorization", "Bearer " + teacher1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.id", is(enr.getId().toString())));

        StudentEnrollment updated = studentEnrollmentRepository.findById(enr.getId()).orElseThrow();
        assertEquals(EnrollmentStatus.ACTIVE, updated.getStatus());
        assertEquals(1, studentEnrollmentRepository.count());
    }

    @Test
    @DisplayName("Teacher student picker returns safe student DTO without passwords or tokens")
    void testTeacherGetsAllStudentsSafeSummary() throws Exception {
        mockMvc.perform(get("/api/teacher/enrollment/students")
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].password").doesNotExist())
                .andExpect(jsonPath("$.data[0].token").doesNotExist())
                .andExpect(jsonPath("$.data[0].email", notNullValue()))
                .andExpect(jsonPath("$.data[0].firstName", notNullValue()));
    }

    // ==========================================
    // STUDENT ENROLLED SUBJECTS TESTS
    // ==========================================

    @Test
    @DisplayName("Student views enrolled subjects returns only active subjects with teacher info")
    void testStudentViewsEnrolledSubjectsWithTeacherInfo() throws Exception {
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(teacher1)
                .subject(subject1)
                .build());

        // Student 1 active in subject1, inactive in subject2
        studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student1)
                .subject(subject1)
                .status(EnrollmentStatus.ACTIVE)
                .build());

        studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student1)
                .subject(subject2)
                .status(EnrollmentStatus.INACTIVE)
                .build());

        mockMvc.perform(get("/api/student/enrollment/subjects")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].subjectId", is(subject1.getId().toString())))
                .andExpect(jsonPath("$.data[0].subjectName", is("Data Structures")))
                .andExpect(jsonPath("$.data[0].teacherFirstName", is("Alice")))
                .andExpect(jsonPath("$.data[0].teacherLastName", is("Smith")))
                .andExpect(jsonPath("$.data[0].teacherEmail", is(teacher1.getEmail())));
    }

    @Test
    @DisplayName("Student views my enrollments returns all student's enrollments")
    void testStudentViewsMyEnrollments() throws Exception {
        studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student1)
                .subject(subject1)
                .status(EnrollmentStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/student/enrollment")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].subjectName", is("Data Structures")))
                .andExpect(jsonPath("$.data[0].status", is("ACTIVE")));
    }
}
