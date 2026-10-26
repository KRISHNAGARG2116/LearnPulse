package com.learnpulse.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.ForgotPasswordRequest;
import com.learnpulse.backend.dto.LoginRequest;
import com.learnpulse.backend.dto.ResetPasswordRequest;
import com.learnpulse.backend.entity.PasswordResetToken;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.repository.PasswordResetTokenRepository;
import com.learnpulse.backend.repository.UserRepository;
import com.learnpulse.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthPasswordResetTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String testUserEmail;
    private final String initialPassword = "InitialPassword123!";

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        testUserEmail = "reset_user_" + suffix + "@learnpulse.ai";

        testUser = userRepository.save(User.builder()
                .email(testUserEmail)
                .password(passwordEncoder.encode(initialPassword))
                .role(Role.STUDENT)
                .enabled(true)
                .build());
    }

    @Test
    @DisplayName("Public Forgot Password endpoint initiates reset and issues token")
    void testForgotPasswordSuccess() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(testUserEmail);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("If an account with that email exists, password reset instructions have been dispatched."));

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findByUserOrderByCreatedAtDesc(testUser);
        assertFalse(tokens.isEmpty(), "A reset token must be persisted in DB");
        PasswordResetToken token = tokens.get(0);
        assertNotNull(token.getTokenHash());
        assertFalse(token.isConsumed());
        assertTrue(token.getExpiryDate().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Forgot Password endpoint returns HTTP 200 for non-existent email (anti-enumeration)")
    void testForgotPasswordNonExistentEmail() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nonexistent_" + UUID.randomUUID() + "@learnpulse.ai");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("If an account with that email exists, password reset instructions have been dispatched."));
    }

    @Test
    @DisplayName("Full Reset Password workflow succeeds and allows login with new password")
    void testResetPasswordWorkflowSuccess() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = AuthService.hashToken(rawToken);

        PasswordResetToken tokenEntity = PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .user(testUser)
                .expiryDate(Instant.now().plus(24, ChronoUnit.HOURS))
                .consumed(false)
                .build();
        passwordResetTokenRepository.save(tokenEntity);

        String newPassword = "BrandNewSecurePassword456!";
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(rawToken);
        request.setNewPassword(newPassword);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Password has been reset successfully. Please log in with your new password."));

        // Verify token marked consumed
        PasswordResetToken updatedToken = passwordResetTokenRepository.findById(tokenEntity.getId()).orElseThrow();
        assertTrue(updatedToken.isConsumed(), "Token must be marked consumed");

        // Verify user can log in with new password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUserEmail);
        loginRequest.setPassword(newPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // Verify old password fails
        loginRequest.setPassword(initialPassword);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Reset Password rejects already consumed token (HTTP 400)")
    void testResetPasswordConsumedTokenFails() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = AuthService.hashToken(rawToken);

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .user(testUser)
                .expiryDate(Instant.now().plus(24, ChronoUnit.HOURS))
                .consumed(true) // already consumed
                .build());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(rawToken);
        request.setNewPassword("AnotherNewPassword789!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password reset token has already been used"));
    }

    @Test
    @DisplayName("Reset Password rejects expired token (HTTP 400)")
    void testResetPasswordExpiredTokenFails() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = AuthService.hashToken(rawToken);

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .user(testUser)
                .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS)) // expired 1 hour ago
                .consumed(false)
                .build());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(rawToken);
        request.setNewPassword("AnotherNewPassword789!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password reset token has expired"));
    }

    @Test
    @DisplayName("Reset Password rejects password identical to current password (HTTP 400)")
    void testResetPasswordSamePasswordFails() throws Exception {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = AuthService.hashToken(rawToken);

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .user(testUser)
                .expiryDate(Instant.now().plus(24, ChronoUnit.HOURS))
                .consumed(false)
                .build());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(rawToken);
        request.setNewPassword(initialPassword); // identical to current

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password cannot be identical to the existing password"));
    }
}
