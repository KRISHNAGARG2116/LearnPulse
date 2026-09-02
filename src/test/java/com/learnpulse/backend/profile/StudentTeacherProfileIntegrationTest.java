package com.learnpulse.backend.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.StudentProfileDTO;
import com.learnpulse.backend.dto.TeacherProfileDTO;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StudentTeacherProfileIntegrationTest {

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

    private User student1;
    private User student2;
    private User teacher1;

    private String student1Token;
    private String student2Token;
    private String teacher1Token;

    @Autowired
    private com.learnpulse.backend.repository.QuizRepository quizRepository;

    @Autowired
    private com.learnpulse.backend.repository.QuestionRepository questionRepository;

    @Autowired
    private com.learnpulse.backend.repository.StudentQuizResultRepository resultRepository;

    @Autowired
    private com.learnpulse.backend.repository.UploadedDocumentRepository documentRepository;

    @Autowired
    private com.learnpulse.backend.repository.NotesRepository notesRepository;

    @BeforeEach
    void setUp() {
        resultRepository.deleteAll();
        questionRepository.deleteAll();
        quizRepository.deleteAll();
        notesRepository.deleteAll();
        documentRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        // Setup Student 1
        student1 = userRepository.save(User.builder()
                .email("student1@learnpulse.ai")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.STUDENT)
                .build());
        userProfileRepository.save(UserProfile.builder()
                .user(student1)
                .firstName("Alice")
                .lastName("Student")
                .enrollmentNumber("STU-001")
                .department("Computer Science")
                .build());
        student1Token = jwtProvider.generateAccessToken(student1);

        // Setup Student 2
        student2 = userRepository.save(User.builder()
                .email("student2@learnpulse.ai")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.STUDENT)
                .build());
        userProfileRepository.save(UserProfile.builder()
                .user(student2)
                .firstName("Bob")
                .lastName("Student")
                .enrollmentNumber("STU-002")
                .department("Electrical")
                .build());
        student2Token = jwtProvider.generateAccessToken(student2);

        // Setup Teacher 1
        teacher1 = userRepository.save(User.builder()
                .email("teacher1@learnpulse.ai")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.TEACHER)
                .build());
        userProfileRepository.save(UserProfile.builder()
                .user(teacher1)
                .firstName("Sarah")
                .lastName("Professor")
                .department("Mathematics")
                .build());
        teacher1Token = jwtProvider.generateAccessToken(teacher1);
    }

    @Test
    @DisplayName("GET /api/student/profile retrieves authenticated student profile")
    void testGetStudentProfileSuccess() throws Exception {
        mockMvc.perform(get("/api/student/profile")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.email", is("student1@learnpulse.ai")))
                .andExpect(jsonPath("$.data.firstName", is("Alice")))
                .andExpect(jsonPath("$.data.enrollmentNumber", is("STU-001")));
    }

    @Test
    @DisplayName("PUT /api/student/profile updates authenticated student profile and enforces ownership")
    void testUpdateStudentProfileOwnershipEnforced() throws Exception {
        StudentProfileDTO updateReq = StudentProfileDTO.builder()
                .firstName("AliceUpdated")
                .lastName("StudentUpdated")
                .enrollmentNumber("STU-001-MOD")
                .department("Artificial Intelligence")
                .build();

        mockMvc.perform(put("/api/student/profile")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.firstName", is("AliceUpdated")))
                .andExpect(jsonPath("$.data.department", is("Artificial Intelligence")));

        // Verify Student 2 profile remains completely untouched
        UserProfile s2Profile = userProfileRepository.findByUserId(student2.getId()).orElseThrow();
        assertEquals("Bob", s2Profile.getFirstName());
    }

    @Test
    @DisplayName("GET /api/teacher/profile retrieves authenticated teacher profile")
    void testGetTeacherProfileSuccess() throws Exception {
        mockMvc.perform(get("/api/teacher/profile")
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.email", is("teacher1@learnpulse.ai")))
                .andExpect(jsonPath("$.data.firstName", is("Sarah")))
                .andExpect(jsonPath("$.data.department", is("Mathematics")));
    }

    @Test
    @DisplayName("PUT /api/teacher/profile updates authenticated teacher profile")
    void testUpdateTeacherProfileSuccess() throws Exception {
        TeacherProfileDTO updateReq = TeacherProfileDTO.builder()
                .firstName("Dr. Sarah")
                .lastName("Professor")
                .department("Advanced Mathematics")
                .build();

        mockMvc.perform(put("/api/teacher/profile")
                        .header("Authorization", "Bearer " + teacher1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.firstName", is("Dr. Sarah")))
                .andExpect(jsonPath("$.data.department", is("Advanced Mathematics")));
    }

    @Test
    @DisplayName("Cross-role profile access rejected (Student accessing Teacher profile returns 403)")
    void testCrossRoleProfileAccessRejected() throws Exception {
        mockMvc.perform(get("/api/teacher/profile")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isForbidden());
    }
}
