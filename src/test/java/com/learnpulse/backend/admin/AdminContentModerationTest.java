package com.learnpulse.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.RejectTeacherRequest;
import com.learnpulse.backend.dto.UpdateContentStatusRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminContentModerationTest {

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
    private UploadedDocumentRepository documentRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private User adminUser;
    private User teacherUser;
    private User studentUser;
    private Subject subject;
    private UploadedDocument document;
    private Quiz quiz;

    private String adminToken;
    private String teacherToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = userRepository.save(User.builder()
                .email("mod_admin_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        teacherUser = userRepository.save(User.builder()
                .email("mod_teacher_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.TEACHER)
                .enabled(true)
                .build());

        userProfileRepository.save(UserProfile.builder()
                .user(teacherUser)
                .firstName("Faculty")
                .lastName("Member")
                .department("Computer Science")
                .teacherVerificationStatus(TeacherVerificationStatus.PENDING)
                .build());

        studentUser = userRepository.save(User.builder()
                .email("mod_student_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.STUDENT)
                .enabled(true)
                .build());

        subject = subjectRepository.save(Subject.builder()
                .name("Moderation Subject " + suffix)
                .code("MOD-" + suffix)
                .build());

        document = documentRepository.save(UploadedDocument.builder()
                .originalFileName("Syllabus_" + suffix + ".pdf")
                .storedFileName("stored_" + suffix + ".pdf")
                .filePath("/tmp/" + suffix + ".pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .teacher(teacherUser)
                .subject(subject)
                .active(true)
                .processingStatus(ProcessingStatus.PROCESSED)
                .build());

        quiz = quizRepository.save(Quiz.builder()
                .title("Quiz on " + subject.getName())
                .subject(subject)
                .totalMarks(10)
                .createdBy(teacherUser)
                .status(QuizStatus.DRAFT)
                .isPublished(false)
                .build());

        adminToken = jwtProvider.generateAccessToken(adminUser);
        teacherToken = jwtProvider.generateAccessToken(teacherUser);
        studentToken = jwtProvider.generateAccessToken(studentUser);
    }

    @Test
    @DisplayName("Admin can list teachers and filter by pending status")
    void testListTeachersAndPending() throws Exception {
        mockMvc.perform(get("/api/admin/teachers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", not(empty())));

        mockMvc.perform(get("/api/admin/teachers/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Admin can approve teacher verification")
    void testApproveTeacher() throws Exception {
        mockMvc.perform(post("/api/admin/teachers/" + teacherUser.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        UserProfile updated = userProfileRepository.findByUserId(teacherUser.getId()).orElseThrow();
        assertEquals(TeacherVerificationStatus.APPROVED, updated.getTeacherVerificationStatus());
        assertNull(updated.getRejectionReason());
    }

    @Test
    @DisplayName("Admin can reject teacher verification with recorded rationale")
    void testRejectTeacher() throws Exception {
        RejectTeacherRequest request = new RejectTeacherRequest("Missing accredited faculty credentials");

        mockMvc.perform(post("/api/admin/teachers/" + teacherUser.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Missing accredited faculty credentials"));

        UserProfile updated = userProfileRepository.findByUserId(teacherUser.getId()).orElseThrow();
        assertEquals(TeacherVerificationStatus.REJECTED, updated.getTeacherVerificationStatus());
        assertEquals("Missing accredited faculty credentials", updated.getRejectionReason());
    }

    @Test
    @DisplayName("Admin can moderate uploaded documents (Toggle active status)")
    void testDocumentModeration() throws Exception {
        // 1. List documents
        mockMvc.perform(get("/api/admin/content/documents")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", not(empty())));

        // 2. Deactivate document
        UpdateContentStatusRequest request = UpdateContentStatusRequest.builder()
                .active(false)
                .build();

        mockMvc.perform(patch("/api/admin/content/documents/" + document.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        UploadedDocument updated = documentRepository.findById(document.getId()).orElseThrow();
        assertFalse(updated.isActive());
    }

    @Test
    @DisplayName("Admin can moderate quizzes (Publish / Unpublish)")
    void testQuizModeration() throws Exception {
        // 1. Publish quiz
        UpdateContentStatusRequest request = UpdateContentStatusRequest.builder()
                .active(true)
                .status("PUBLISHED")
                .build();

        mockMvc.perform(patch("/api/admin/content/quizzes/" + quiz.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.isPublished").value(true));

        Quiz updated = quizRepository.findById(quiz.getId()).orElseThrow();
        assertEquals(QuizStatus.PUBLISHED, updated.getStatus());
        assertTrue(updated.getIsPublished());
    }

    @Test
    @DisplayName("Non-admin roles forbidden from content moderation (HTTP 403)")
    void testNonAdminForbiddenFromModeration() throws Exception {
        mockMvc.perform(get("/api/admin/teachers")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/content/documents")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/content/quizzes")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }
}
