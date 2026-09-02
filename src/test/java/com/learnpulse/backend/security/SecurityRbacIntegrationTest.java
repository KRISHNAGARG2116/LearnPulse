package com.learnpulse.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.LoginRequest;
import com.learnpulse.backend.dto.RefreshTokenRequest;
import com.learnpulse.backend.dto.RegisterRequest;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.entity.UserProfile;
import com.learnpulse.backend.repository.UserProfileRepository;
import com.learnpulse.backend.repository.UserRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.learnpulse.backend.repository.UploadedDocumentRepository documentRepository;

    @Autowired
    private com.learnpulse.backend.repository.NotesRepository notesRepository;

    @Autowired
    private com.learnpulse.backend.repository.QuizRepository quizRepository;

    @Autowired
    private com.learnpulse.backend.repository.QuestionRepository questionRepository;

    @Autowired
    private com.learnpulse.backend.repository.StudentQuizResultRepository resultRepository;

    @BeforeEach
    void cleanUp() {
        resultRepository.deleteAll();
        questionRepository.deleteAll();
        quizRepository.deleteAll();
        notesRepository.deleteAll();
        documentRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("TEST 1: User Registration succeeds for STUDENT role and stores BCrypt hashed password")
    void testUserRegistrationSuccess() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("student@learnpulse.ai")
                .password("Password123!")
                .firstName("John")
                .lastName("Doe")
                .role(Role.STUDENT)
                .enrollmentNumber("STU-1001")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.email", is("student@learnpulse.ai")))
                .andExpect(jsonPath("$.data.role", is("STUDENT")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()));

        User savedUser = userRepository.findByEmail("student@learnpulse.ai").orElseThrow();
        assertNotEquals("Password123!", savedUser.getPassword());
        assertTrue(passwordEncoder.matches("Password123!", savedUser.getPassword()));
    }

    @Test
    @DisplayName("TEST 1b: Duplicate email registration fails with HTTP 400 Bad Request")
    void testDuplicateEmailRegistrationFails() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("duplicate@learnpulse.ai")
                .password("Password123!")
                .firstName("John")
                .lastName("Doe")
                .role(Role.STUDENT)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @DisplayName("SECURITY AUDIT TEST: Public registration attempting ADMIN role is rejected with HTTP 403 Forbidden")
    void testPublicAdminRegistrationFails() throws Exception {
        RegisterRequest adminAttackerReq = RegisterRequest.builder()
                .email("attacker@learnpulse.ai")
                .password("HackerPass123!")
                .firstName("Malicious")
                .lastName("User")
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminAttackerReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("Public registration as ADMIN role is not permitted")));

        assertTrue(userRepository.findByEmail("attacker@learnpulse.ai").isEmpty());
    }

    @Test
    @DisplayName("TEST 2 & 3: Valid Login issues JWT tokens, Invalid Login returns HTTP 401 Unauthorized")
    void testLoginValidAndInvalid() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("teacher@learnpulse.ai")
                .password("TeacherPass123!")
                .firstName("Sarah")
                .lastName("Conner")
                .role(Role.TEACHER)
                .department("Computer Science")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Valid Login
        LoginRequest validLogin = LoginRequest.builder()
                .email("teacher@learnpulse.ai")
                .password("TeacherPass123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.role", is("TEACHER")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));

        // Invalid Password Login
        LoginRequest invalidLogin = LoginRequest.builder()
                .email("teacher@learnpulse.ai")
                .password("WrongPassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    @DisplayName("TEST 5: Refresh token workflow issues a new access token")
    void testRefreshTokenWorkflow() throws Exception {
        User adminUser = userRepository.save(User.builder()
                .email("admin_refresh@learnpulse.ai")
                .password(passwordEncoder.encode("AdminSecret123!"))
                .role(Role.ADMIN)
                .build());
        userProfileRepository.save(UserProfile.builder().user(adminUser).firstName("System").lastName("Admin").build());

        String refreshToken = jwtProvider.generateRefreshToken(adminUser);

        RefreshTokenRequest refreshReq = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));
    }

    @Test
    @DisplayName("TEST 7: Unauthenticated request to protected endpoint returns HTTP 401 Unauthorized")
    void testUnauthenticatedAccessFails() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    @DisplayName("TEST 8 & 9: Role-Based Access Control and Privilege Escalation Rejection")
    void testRbacAndPrivilegeEscalationRejection() throws Exception {
        // Register STUDENT via public API
        RegisterRequest studentReq = RegisterRequest.builder()
                .email("student_rbac@learnpulse.ai")
                .password("StudentPass123!")
                .firstName("Bob")
                .lastName("Smith")
                .role(Role.STUDENT)
                .build();

        MvcResult studentReg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String studentToken = objectMapper.readTree(studentReg.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Create ADMIN via repository (Simulating administrative provisioning)
        User adminUser = userRepository.save(User.builder()
                .email("admin_rbac@learnpulse.ai")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ADMIN)
                .build());
        userProfileRepository.save(UserProfile.builder().user(adminUser).firstName("Alice").lastName("Boss").build());

        String adminToken = jwtProvider.generateAccessToken(adminUser);

        // 1. STUDENT accesses /api/student/test -> 200 OK
        mockMvc.perform(get("/api/student/test")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.role", is("STUDENT")));

        // 2. ADMIN accesses /api/admin/test -> 200 OK
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.role", is("ADMIN")));

        // 3. Privilege Escalation Rejection: STUDENT attempts to access /api/admin/test -> 403 Forbidden
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is("error")));

        // 4. Privilege Escalation Rejection: STUDENT attempts to access /api/teacher/test -> 403 Forbidden
        mockMvc.perform(get("/api/teacher/test")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is("error")));
    }
}
