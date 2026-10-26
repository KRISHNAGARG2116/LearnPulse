package com.learnpulse.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.UpdateUserProfileRequest;
import com.learnpulse.backend.dto.UpdateUserRoleRequest;
import com.learnpulse.backend.entity.PasswordResetToken;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.entity.UserProfile;
import com.learnpulse.backend.repository.PasswordResetTokenRepository;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserManagementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private User adminUser;
    private User secondaryAdminUser;
    private User teacherUser;
    private User studentUser;

    private String adminToken;
    private String teacherToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = userRepository.save(User.builder()
                .email("admin_" + uniqueSuffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        secondaryAdminUser = userRepository.save(User.builder()
                .email("admin2_" + uniqueSuffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        teacherUser = userRepository.save(User.builder()
                .email("teacher_" + uniqueSuffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("TeacherPass123!"))
                .role(Role.TEACHER)
                .enabled(true)
                .build());

        studentUser = userRepository.save(User.builder()
                .email("student_" + uniqueSuffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("StudentPass123!"))
                .role(Role.STUDENT)
                .enabled(true)
                .build());

        userProfileRepository.save(UserProfile.builder()
                .user(studentUser)
                .firstName("Test")
                .lastName("Student")
                .department("CS")
                .enrollmentNumber("ENR-1001")
                .build());

        adminToken = jwtProvider.generateAccessToken(adminUser);
        teacherToken = jwtProvider.generateAccessToken(teacherUser);
        studentToken = jwtProvider.generateAccessToken(studentUser);
    }

    @Test
    @DisplayName("Admin can list users with pagination and search")
    void testAdminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(4)));
    }

    @Test
    @DisplayName("Admin can filter users by role")
    void testFilterUsersByRole() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("role", "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].role", everyItem(is("TEACHER"))));
    }

    @Test
    @DisplayName("Non-admin roles are forbidden from accessing admin user management (HTTP 403)")
    void testNonAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated requests return HTTP 401")
    void testUnauthenticatedForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin can view detailed user profile with activity metrics")
    void testGetUserDetail() throws Exception {
        mockMvc.perform(get("/api/admin/users/" + studentUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(studentUser.getEmail()))
                .andExpect(jsonPath("$.data.firstName").value("Test"))
                .andExpect(jsonPath("$.data.lastName").value("Student"))
                .andExpect(jsonPath("$.data.department").value("CS"))
                .andExpect(jsonPath("$.data.enrolledSubjectsCount", notNullValue()));
    }

    @Test
    @DisplayName("Admin can update user profile fields")
    void testUpdateUserProfile() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .firstName("UpdatedFirst")
                .lastName("UpdatedLast")
                .department("Software Engineering")
                .build();

        mockMvc.perform(patch("/api/admin/users/" + studentUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("UpdatedFirst"))
                .andExpect(jsonPath("$.data.lastName").value("UpdatedLast"))
                .andExpect(jsonPath("$.data.department").value("Software Engineering"));
    }

    @Test
    @DisplayName("Admin can update user role")
    void testUpdateUserRole() throws Exception {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.TEACHER);

        mockMvc.perform(patch("/api/admin/users/" + studentUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("TEACHER"));

        User reloaded = userRepository.findById(studentUser.getId()).orElseThrow();
        assertEquals(Role.TEACHER, reloaded.getRole());
    }

    @Test
    @DisplayName("Administrator cannot demote themselves (Self-demotion protection)")
    void testSelfDemotionForbidden() throws Exception {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.STUDENT);

        mockMvc.perform(patch("/api/admin/users/" + adminUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot change their own role")));
    }

    @Test
    @DisplayName("Cannot demote last active administrator (Last admin rule)")
    void testLastAdminDemotionForbidden() throws Exception {
        // Deactivate secondary admin so only 1 active admin remains
        secondaryAdminUser.setEnabled(false);
        userRepository.save(secondaryAdminUser);

        // Attempting to demote secondaryAdminUser when role was changed
        secondaryAdminUser.setEnabled(true);
        userRepository.save(secondaryAdminUser);

        // Deactivate adminUser
        userRepository.delete(adminUser);

        // Now secondaryAdminUser is the only active admin
        String secondaryAdminToken = jwtProvider.generateAccessToken(secondaryAdminUser);

        // Create a temporary third user to act as admin
        User thirdAdmin = userRepository.save(User.builder()
                .email("admin3_" + UUID.randomUUID() + "@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());
        String thirdAdminToken = jwtProvider.generateAccessToken(thirdAdmin);

        // Now demote secondaryAdminUser
        mockMvc.perform(patch("/api/admin/users/" + secondaryAdminUser.getId() + "/role")
                        .header("Authorization", "Bearer " + thirdAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRoleRequest(Role.TEACHER))))
                .andExpect(status().isOk());

        // Now thirdAdmin is the ONLY active admin left. Attempting to demote an admin user should fail with 409 Conflict:
        User fourthAdmin = userRepository.save(User.builder()
                .email("admin4_" + UUID.randomUUID() + "@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.ADMIN)
                .enabled(false) // disabled
                .build());

        // Ensure thirdAdmin is the ONLY active admin across the database
        List<User> otherAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.isEnabled() && !u.getId().equals(thirdAdmin.getId()))
                .toList();
        otherAdmins.forEach(u -> u.setEnabled(false));
        userRepository.saveAll(otherAdmins);

        try {
            // Demoting fourthAdmin when thirdAdmin is the only active admin remaining fails with 409
            mockMvc.perform(patch("/api/admin/users/" + fourthAdmin.getId() + "/role")
                            .header("Authorization", "Bearer " + thirdAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserRoleRequest(Role.STUDENT))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("Cannot demote the last remaining active administrator")));
        } finally {
            otherAdmins.forEach(u -> u.setEnabled(true));
            userRepository.saveAll(otherAdmins);
        }
    }

    @Test
    @DisplayName("Admin can initiate password reset with generic response and DB token persistence")
    void testInitiatePasswordReset() throws Exception {
        mockMvc.perform(post("/api/admin/users/" + studentUser.getId() + "/password-reset")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.initiated").value(true))
                .andExpect(jsonPath("$.data.message", containsString("dispatched securely")));

        // Token must be stored in database, not returned in response
        var tokens = passwordResetTokenRepository.findAll();
        assertFalse(tokens.isEmpty());
        PasswordResetToken stored = tokens.get(0);
        assertEquals(studentUser.getId(), stored.getUser().getId());
        assertNotNull(stored.getTokenHash());
        assertFalse(stored.isConsumed());
    }

    @Test
    @DisplayName("Admin can deactivate and reactivate a user account")
    void testDeactivateAndReactivateUser() throws Exception {
        // 1. Deactivate
        mockMvc.perform(patch("/api/admin/users/" + studentUser.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        User deactivated = userRepository.findById(studentUser.getId()).orElseThrow();
        assertFalse(deactivated.isEnabled());

        // 2. Immediate Session Revocation Check:
        // Request made using studentToken must now be rejected by JwtAuthenticationFilter!
        mockMvc.perform(get("/api/student/dashboard")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isUnauthorized());

        // 3. Reactivate
        mockMvc.perform(patch("/api/admin/users/" + studentUser.getId() + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        User reactivated = userRepository.findById(studentUser.getId()).orElseThrow();
        assertTrue(reactivated.isEnabled());
    }

    @Test
    @DisplayName("Admin cannot deactivate themselves (Self-deactivation protection)")
    void testSelfDeactivationForbidden() throws Exception {
        mockMvc.perform(patch("/api/admin/users/" + adminUser.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot deactivate their own account")));
    }
}
