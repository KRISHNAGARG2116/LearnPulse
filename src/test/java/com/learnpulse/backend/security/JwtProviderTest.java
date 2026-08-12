package com.learnpulse.backend.security;

import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long ACCESS_EXPIRATION = 900000L; // 15 mins
    private static final long REFRESH_EXPIRATION = 604800000L; // 7 days

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("student@learnpulse.ai")
                .password("hashed_password")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    @DisplayName("Generate access token and extract correct claims")
    void testGenerateAccessTokenSuccess() {
        String token = jwtProvider.generateAccessToken(testUser);

        assertNotNull(token);
        assertTrue(jwtProvider.validateToken(token));
        assertEquals("student@learnpulse.ai", jwtProvider.getUsernameFromToken(token));
        assertEquals("STUDENT", jwtProvider.getRoleFromToken(token));
        assertEquals(testUser.getId().toString(), jwtProvider.getUserIdFromToken(token));
        assertEquals(JwtProvider.TOKEN_TYPE_ACCESS, jwtProvider.getTokenTypeFromToken(token));
    }

    @Test
    @DisplayName("Generate refresh token with REFRESH tokenType claim")
    void testGenerateRefreshTokenSuccess() {
        String refreshToken = jwtProvider.generateRefreshToken(testUser);

        assertNotNull(refreshToken);
        assertTrue(jwtProvider.validateToken(refreshToken));
        assertEquals("student@learnpulse.ai", jwtProvider.getUsernameFromToken(refreshToken));
        assertEquals(JwtProvider.TOKEN_TYPE_REFRESH, jwtProvider.getTokenTypeFromToken(refreshToken));
    }

    @Test
    @DisplayName("Reject invalid token signature or malformed token string")
    void testValidateInvalidToken() {
        String malformedToken = "eyJhbGciOiJIUzI1NiJ9.invalid_payload.invalid_signature";
        assertFalse(jwtProvider.validateToken(malformedToken));
    }
}
