package com.learnpulse.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.EnrollStudentRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TeacherVerificationEnforcementTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private User approvedTeacher;
    private User pendingTeacher;
    private User rejectedTeacher;

    private String approvedTeacherToken;
    private String pendingTeacherToken;
    private String rejectedTeacherToken;

    private Subject testSubject;
    private Chapter testChapter;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        approvedTeacher = userRepository.save(User.builder()
                .email("approved_t_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.TEACHER)
                .enabled(true)
                .build());

        userProfileRepository.save(UserProfile.builder()
                .user(approvedTeacher)
                .firstName("Approved")
                .lastName("Teacher")
                .teacherVerificationStatus(TeacherVerificationStatus.APPROVED)
                .build());

        pendingTeacher = userRepository.save(User.builder()
                .email("pending_t_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.TEACHER)
                .enabled(true)
                .build());

        userProfileRepository.save(UserProfile.builder()
                .user(pendingTeacher)
                .firstName("Pending")
                .lastName("Teacher")
                .teacherVerificationStatus(TeacherVerificationStatus.PENDING)
                .build());

        rejectedTeacher = userRepository.save(User.builder()
                .email("rejected_t_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.TEACHER)
                .enabled(true)
                .build());

        userProfileRepository.save(UserProfile.builder()
                .user(rejectedTeacher)
                .firstName("Rejected")
                .lastName("Teacher")
                .teacherVerificationStatus(TeacherVerificationStatus.REJECTED)
                .rejectionReason("Incomplete institutional credential documentation")
                .build());

        approvedTeacherToken = jwtProvider.generateAccessToken(approvedTeacher);
        pendingTeacherToken = jwtProvider.generateAccessToken(pendingTeacher);
        rejectedTeacherToken = jwtProvider.generateAccessToken(rejectedTeacher);

        testSubject = subjectRepository.save(Subject.builder()
                .name("Verification Test Subject " + suffix)
                .code("VTS" + suffix.toUpperCase())
                .build());

        testChapter = chapterRepository.save(Chapter.builder()
                .subject(testSubject)
                .title("Verification Test Chapter")
                .chapterNumber(1)
                .build());

        // Assign approved teacher to subject
        teacherSubjectRepository.save(TeacherSubject.builder()
                .teacher(approvedTeacher)
                .subject(testSubject)
                .build());
    }

    @Test
    @DisplayName("Pending teacher is blocked from uploading notes (HTTP 403 Forbidden)")
    void testPendingTeacherUploadNoteBlocked() throws Exception {
        mockMvc.perform(multipart("/api/teacher/upload-note")
                        .param("title", "Physics Intro Note")
                        .param("content", "This is sample note content for test.")
                        .param("subjectId", testSubject.getId().toString())
                        .param("chapterId", testChapter.getId().toString())
                        .header("Authorization", "Bearer " + pendingTeacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Teacher account verification is pending administrator approval.")));
    }

    @Test
    @DisplayName("Rejected teacher is blocked from uploading notes (HTTP 403 Forbidden)")
    void testRejectedTeacherUploadNoteBlocked() throws Exception {
        mockMvc.perform(multipart("/api/teacher/upload-note")
                        .param("title", "Physics Intro Note")
                        .param("content", "This is sample note content for test.")
                        .param("subjectId", testSubject.getId().toString())
                        .param("chapterId", testChapter.getId().toString())
                        .header("Authorization", "Bearer " + rejectedTeacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Teacher account verification was rejected.")));
    }

    @Test
    @DisplayName("Pending teacher is blocked from publishing quizzes (HTTP 403 Forbidden)")
    void testPendingTeacherPublishQuizBlocked() throws Exception {
        mockMvc.perform(post("/api/teacher/quizzes/" + UUID.randomUUID() + "/publish")
                        .header("Authorization", "Bearer " + pendingTeacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Teacher account verification is pending administrator approval.")));
    }

    @Test
    @DisplayName("Pending teacher is blocked from AI quiz generation (HTTP 403 Forbidden)")
    void testPendingTeacherAiQuizGenerationBlocked() throws Exception {
        GenerateQuizRequest request = new GenerateQuizRequest();
        request.setDocumentId(UUID.randomUUID());
        request.setQuestionCount(5);
        request.setDifficulty("MEDIUM");

        mockMvc.perform(post("/api/ai/generate-quiz")
                        .header("Authorization", "Bearer " + pendingTeacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Teacher account verification is pending administrator approval.")));
    }

    @Test
    @DisplayName("Pending teacher is blocked from student enrollment management (HTTP 403 Forbidden)")
    void testPendingTeacherStudentEnrollmentBlocked() throws Exception {
        EnrollStudentRequest request = new EnrollStudentRequest();
        request.setStudentId(UUID.randomUUID());

        mockMvc.perform(post("/api/teacher/enrollment/subjects/" + testSubject.getId() + "/students")
                        .header("Authorization", "Bearer " + pendingTeacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Teacher account verification is pending administrator approval.")));
    }

    @Test
    @DisplayName("Approved teacher passes verification guard on note upload")
    void testApprovedTeacherUploadNoteAllowed() throws Exception {
        mockMvc.perform(multipart("/api/teacher/upload-note")
                        .param("title", "Approved Teacher Note")
                        .param("content", "This is verified content uploaded by approved teacher.")
                        .param("subjectId", testSubject.getId().toString())
                        .param("chapterId", testChapter.getId().toString())
                        .header("Authorization", "Bearer " + approvedTeacherToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"));
    }
}
