package com.learnpulse.backend.admin;

import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.repository.AiTelemetryRepository;
import com.learnpulse.backend.repository.UserRepository;
import com.learnpulse.backend.security.jwt.JwtProvider;
import com.learnpulse.backend.service.AiTelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiTelemetryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AiTelemetryService aiTelemetryService;

    @Autowired
    private AiTelemetryRepository aiTelemetryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private User adminUser;
    private User studentUser;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        adminUser = userRepository.save(User.builder()
                .email("telem_admin_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        studentUser = userRepository.save(User.builder()
                .email("telem_student_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.STUDENT)
                .enabled(true)
                .build());

        adminToken = jwtProvider.generateAccessToken(adminUser);
        studentToken = jwtProvider.generateAccessToken(studentUser);

        // Seed some sample telemetry
        Instant start1 = Instant.now().minusSeconds(5);
        Instant comp1 = Instant.now().minusSeconds(4);
        aiTelemetryService.recordOperation("AI_TUTOR_CHAT", "qwen2.5-coder:7b", studentUser, start1, comp1, "SUCCESS", 120, 240, null);

        Instant start2 = Instant.now().minusSeconds(3);
        Instant comp2 = Instant.now().minusSeconds(2);
        aiTelemetryService.recordOperation("AI_QUIZ_GENERATION", "qwen2.5-coder:7b", adminUser, start2, comp2, "SUCCESS", 400, 300, null);
    }

    @Test
    @DisplayName("Admin can retrieve aggregated AI usage telemetry summary")
    void testAiUsageSummary() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/ai-usage")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.totalOperations", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.successfulOperations", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.successRatePercentage", greaterThan(0.0)))
                .andExpect(jsonPath("$.data.averageLatencyMs", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.data.totalTokens", greaterThanOrEqualTo(1000)))
                .andExpect(jsonPath("$.data.operationsByFeature", notNullValue()));
    }

    @Test
    @DisplayName("Admin can retrieve paginated AI operational telemetry logs")
    void testAiTelemetryPaginatedLog() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/ai-telemetry")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[0].feature", notNullValue()))
                .andExpect(jsonPath("$.data.content[0].executionLatencyMs", notNullValue()))
                .andExpect(jsonPath("$.data.content[0].provider").value("Ollama"));
    }

    @Test
    @DisplayName("Telemetry persistence failures do not propagate or fail caller (Failure Isolation)")
    void testTelemetryFailureIsolation() {
        assertDoesNotThrow(() -> {
            aiTelemetryService.recordOperation(
                    null, // invalid null feature
                    null,
                    null,
                    Instant.now(),
                    Instant.now(),
                    "FAILED",
                    null,
                    null,
                    "Simulated error"
            );
        });
    }

    @Test
    @DisplayName("Non-admin role forbidden from AI telemetry (HTTP 403)")
    void testNonAdminForbiddenFromTelemetry() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/ai-usage")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/analytics/ai-telemetry")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AI Telemetry records explicit usage provenance and zero direct billing cost for Ollama")
    void testTelemetryProvenanceAndCostAccounting() {
        Instant start = Instant.now().minusSeconds(2);
        Instant comp = Instant.now();

        // 1. Record with LOCAL_ESTIMATION
        aiTelemetryService.recordOperation("AI_TUTOR_CHAT", "qwen2.5-coder:7b", studentUser,
                start, comp, "SUCCESS", 50, 100, "LOCAL_ESTIMATION", null);

        // 2. Record with PROVIDER_REPORTED
        aiTelemetryService.recordOperation("CODE_EXPLAINER", "qwen2.5-coder:7b", studentUser,
                start, comp, "SUCCESS", 80, 160, "PROVIDER_REPORTED", null);

        var records = aiTelemetryRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 10));
        var list = records.getContent();

        assertTrue(list.stream().anyMatch(r -> "LOCAL_ESTIMATION".equals(r.getUsageProvenance())));
        assertTrue(list.stream().anyMatch(r -> "PROVIDER_REPORTED".equals(r.getUsageProvenance())));

        // Verify Ollama provider direct billing cost is $0.00
        for (var rec : list) {
            if ("Ollama".equalsIgnoreCase(rec.getProvider())) {
                assertEquals(0.0, rec.getEstimatedCostUsd());
            }
        }
    }
}
