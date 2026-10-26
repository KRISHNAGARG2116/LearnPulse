package com.learnpulse.backend.admin;

import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.Subject;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.repository.SubjectRepository;
import com.learnpulse.backend.repository.UserRepository;
import com.learnpulse.backend.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAnalyticsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

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
                .email("analytics_admin_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        studentUser = userRepository.save(User.builder()
                .email("analytics_student_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.STUDENT)
                .enabled(true)
                .build());

        adminToken = jwtProvider.generateAccessToken(adminUser);
        studentToken = jwtProvider.generateAccessToken(studentUser);
    }

    @Test
    @DisplayName("Admin can retrieve platform summary metrics")
    void testPlatformSummaryMetrics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.totalUsers", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.activeUsers", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.studentsCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.adminsCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalSubjects", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.totalQuizzes", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.averageQuizScore", greaterThanOrEqualTo(0.0)));
    }

    @Test
    @DisplayName("Admin can retrieve 14-day user registration trends")
    void testUserActivityTrends() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/user-activity")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", hasSize(14)))
                .andExpect(jsonPath("$.data[0].date", notNullValue()))
                .andExpect(jsonPath("$.data[0].registrationsCount", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("Admin can retrieve course activity breakdown")
    void testCourseActivity() throws Exception {
        // Ensure at least one subject exists
        if (subjectRepository.count() == 0) {
            subjectRepository.save(Subject.builder()
                    .name("Analytics Test Subject " + UUID.randomUUID())
                    .code("ATS-" + UUID.randomUUID().toString().substring(0, 4))
                    .build());
        }

        mockMvc.perform(get("/api/admin/analytics/course-activity")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[0].subjectName", notNullValue()))
                .andExpect(jsonPath("$.data[0].enrolledStudentsCount", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("Non-admin role cannot access analytics endpoints (HTTP 403)")
    void testNonAdminForbiddenFromAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/summary")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/analytics/user-activity")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/analytics/course-activity")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }
}
