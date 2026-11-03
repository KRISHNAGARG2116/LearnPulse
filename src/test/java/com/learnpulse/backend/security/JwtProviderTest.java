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

    @Test
    @DisplayName("SEC-02: Reject null or empty JWT secret")
    void testValidateSecretNullOrEmpty() {
        IllegalStateException exNull = assertThrows(IllegalStateException.class, () ->
                JwtProvider.validateSecret(null, false));
        assertTrue(exNull.getMessage().contains("Secret key must not be null, empty, or blank"));

        IllegalStateException exBlank = assertThrows(IllegalStateException.class, () ->
                JwtProvider.validateSecret("   ", false));
        assertTrue(exBlank.getMessage().contains("Secret key must not be null, empty, or blank"));
    }

    @Test
    @DisplayName("SEC-02: Reject invalid Base64 secret")
    void testValidateSecretInvalidBase64() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                JwtProvider.validateSecret("Not-Valid-Base64-Characters!@#$%", false));
        assertTrue(ex.getMessage().contains("Secret key must be a valid BASE64-encoded string"));
    }

    @Test
    @DisplayName("SEC-02: Reject weak secret with less than 256 bits (32 bytes)")
    void testValidateSecretTooShort() {
        // 16 bytes base64 encoded = 128 bits (too short for HS256)
        String shortSecretBase64 = "MDEyMzQ1Njc4OWFiY2RlZg=="; // 16 bytes
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                JwtProvider.validateSecret(shortSecretBase64, false));
        assertTrue(ex.getMessage().contains("below the required 256 bits"));
    }

    @Test
    @DisplayName("SEC-02: Reject default development secret in production mode")
    void testValidateSecretDefaultInProduction() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                JwtProvider.validateSecret(JwtProvider.DEFAULT_DEV_SECRET, true));
        assertTrue(ex.getMessage().contains("The default development JWT secret cannot be used in a production environment"));
    }

    @Test
    @DisplayName("SEC-02: Allow default development secret in non-production mode")
    void testValidateSecretDefaultInDevelopment() {
        assertDoesNotThrow(() ->
                JwtProvider.validateSecret(JwtProvider.DEFAULT_DEV_SECRET, false));
    }

    @Test
    @DisplayName("SEC-02: Allow strong valid secret in production mode")
    void testValidateSecretStrongInProduction() {
        // 32 bytes of secure random hex encoded in base64: 256 bits
        String strongSecret = "YjRhM2M0ZTVmNmE3YjhkOWUxZjJhM2I0YzVkNmU3ZjhhOWIxYzJkM2U0ZjVhNmI3";
        assertDoesNotThrow(() ->
                JwtProvider.validateSecret(strongSecret, true));

        // Verify tokens can be created with strong production secret
        JwtProvider prodProvider = new JwtProvider(strongSecret, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
        String token = prodProvider.generateAccessToken(testUser);
        assertNotNull(token);
        assertTrue(prodProvider.validateToken(token));
        assertEquals("student@learnpulse.ai", prodProvider.getUsernameFromToken(token));
    }
}
