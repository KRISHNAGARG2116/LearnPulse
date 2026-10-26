package com.learnpulse.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpulse.backend.dto.UpdateUserRoleRequest;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private User admin1;
    private User admin2;
    private String admin1Token;
    private String admin2Token;
    private List<User> deactivatedOtherAdmins = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Isolate exact active admin count: deactivate existing admins other than our test pair
        List<User> existingAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.isEnabled())
                .toList();

        for (User u : existingAdmins) {
            u.setEnabled(false);
            userRepository.save(u);
            deactivatedOtherAdmins.add(u);
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        admin1 = userRepository.save(User.builder()
                .email("admin_conc_1_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        admin2 = userRepository.save(User.builder()
                .email("admin_conc_2_" + suffix + "@learnpulse.ai")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        admin1Token = jwtProvider.generateAccessToken(admin1);
        admin2Token = jwtProvider.generateAccessToken(admin2);
    }

    @Test
    @DisplayName("Pessimistic locking serializes concurrent demotions and preserves last active administrator")
    void testConcurrentDemotionPreventsZeroAdmins() throws Exception {
        try {
            int threadCount = 2;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CyclicBarrier barrier = new CyclicBarrier(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger conflictCount = new AtomicInteger(0);

            // Admin 2 attempts to demote Admin 1 (not self-demotion)
            Callable<Integer> admin2DemotesAdmin1 = () -> {
                barrier.await();
                UpdateUserRoleRequest req = new UpdateUserRoleRequest();
                req.setRole(Role.TEACHER);
                MvcResult result = mockMvc.perform(patch("/api/admin/users/" + admin1.getId() + "/role")
                                .header("Authorization", "Bearer " + admin2Token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andReturn();
                return result.getResponse().getStatus();
            };

            // Admin 1 attempts to demote Admin 2 (not self-demotion)
            Callable<Integer> admin1DemotesAdmin2 = () -> {
                barrier.await();
                UpdateUserRoleRequest req = new UpdateUserRoleRequest();
                req.setRole(Role.TEACHER);
                MvcResult result = mockMvc.perform(patch("/api/admin/users/" + admin2.getId() + "/role")
                                .header("Authorization", "Bearer " + admin1Token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andReturn();
                return result.getResponse().getStatus();
            };

            Future<Integer> f1 = executor.submit(admin2DemotesAdmin1);
            Future<Integer> f2 = executor.submit(admin1DemotesAdmin2);

            int status1 = f1.get(10, TimeUnit.SECONDS);
            int status2 = f2.get(10, TimeUnit.SECONDS);

            executor.shutdown();

            if (status1 == 200) successCount.incrementAndGet();
            if (status1 == 409) conflictCount.incrementAndGet();

            if (status2 == 200) successCount.incrementAndGet();
            if (status2 == 409) conflictCount.incrementAndGet();

            // Exactly one should succeed (200) and the other MUST conflict (409)
            assertEquals(1, successCount.get(), "Exactly one admin demotion must succeed");
            assertEquals(1, conflictCount.get(), "The second concurrent demotion must be blocked with HTTP 409 Conflict");

            // Verify database state: at least one active admin MUST remain
            long remainingActiveAdmins = userRepository.countByRoleAndEnabled(Role.ADMIN, true);
            assertTrue(remainingActiveAdmins >= 1, "Database must retain at least 1 active administrator");
        } finally {
            // Restore other admins
            for (User u : deactivatedOtherAdmins) {
                u.setEnabled(true);
                userRepository.save(u);
            }
        }
    }
}
