package com.learnpulse.backend.security.jwt;

import com.learnpulse.backend.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtProvider {

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    /**
     * Known development default HMAC secret key.
     * MUST NOT be permitted in production environments (SEC-02 hardening).
     */
    public static final String DEFAULT_DEV_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    /**
     * Minimum required key length for HMAC-SHA256 (256 bits = 32 bytes).
     */
    public static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    @Autowired
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs,
            @Autowired(required = false) Environment environment) {
        boolean isProduction = isProductionEnvironment(environment);
        validateSecret(secret, isProduction);

        if (!isProduction && DEFAULT_DEV_SECRET.equals(secret != null ? secret.trim() : "")) {
            log.warn("SECURITY WARNING: JwtProvider initialized with default development secret. Do not use this configuration in production!");
        }

        byte[] keyBytes = Decoders.BASE64.decode(secret.trim());
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Backward-compatible constructor for testing.
     */
    public JwtProvider(String secret, long accessTokenExpirationMs, long refreshTokenExpirationMs) {
        this(secret, accessTokenExpirationMs, refreshTokenExpirationMs, null);
    }

    /**
     * Validates that the provided JWT secret meets security requirements.
     * Prevents empty, malformed, weak (< 256 bits), or default secrets in production.
     * The secret value itself is NEVER included in error messages or logs.
     *
     * @param secret Base64-encoded secret key
     * @param isProduction whether the active environment is production
     * @throws IllegalStateException if the secret is invalid or insecure for the active profile
     */
    public static void validateSecret(String secret, boolean isProduction) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret configuration error: Secret key must not be null, empty, or blank");
        }

        String trimmed = secret.trim();

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(trimmed);
        } catch (Exception ex) {
            throw new IllegalStateException("JWT secret configuration error: Secret key must be a valid BASE64-encoded string");
        }

        if (keyBytes == null || keyBytes.length < MIN_SECRET_BYTES) {
            int bits = (keyBytes != null) ? keyBytes.length * 8 : 0;
            throw new IllegalStateException("JWT secret configuration error: Secret key strength (" + bits + " bits) is below the required 256 bits (32 bytes)");
        }

        if (isProduction && DEFAULT_DEV_SECRET.equals(trimmed)) {
            throw new IllegalStateException("JWT secret configuration error: The default development JWT secret cannot be used in a production environment. Configure a unique JWT_SECRET via environment variables.");
        }
    }

    private static boolean isProductionEnvironment(Environment environment) {
        if (environment == null) {
            return false;
        }
        String[] profiles = environment.getActiveProfiles();
        if (profiles == null || profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        return Arrays.stream(profiles)
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId().toString());
        claims.put("tokenType", TOKEN_TYPE_ACCESS);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId().toString());
        claims.put("tokenType", TOKEN_TYPE_REFRESH);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String getUserIdFromToken(String token) {
        return getClaims(token).get("userId", String.class);
    }

    public String getTokenTypeFromToken(String token) {
        return getClaims(token).get("tokenType", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token format: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
