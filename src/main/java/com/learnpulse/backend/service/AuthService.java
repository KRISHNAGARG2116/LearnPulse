package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.AuthResponse;
import com.learnpulse.backend.dto.LoginRequest;
import com.learnpulse.backend.dto.RefreshTokenRequest;
import com.learnpulse.backend.dto.RegisterRequest;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.entity.UserProfile;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.repository.UserProfileRepository;
import com.learnpulse.backend.repository.UserRepository;
import com.learnpulse.backend.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.learnpulse.backend.dto.ForgotPasswordRequest;
import com.learnpulse.backend.dto.ResetPasswordRequest;
import com.learnpulse.backend.entity.PasswordResetToken;
import com.learnpulse.backend.entity.TeacherVerificationStatus;
import com.learnpulse.backend.repository.PasswordResetTokenRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email address is already registered", HttpStatus.BAD_REQUEST);
        }

        // Security Audit Fix: Prevent public self-assignment of ADMIN role (Privilege Escalation Protection)
        Role assignedRole = request.getRole();
        if (assignedRole == Role.ADMIN) {
            log.warn("Security Alert: Unauthorized attempt to publicly register ADMIN account with email: {}", request.getEmail());
            throw new ApiException("Public registration as ADMIN role is not permitted", HttpStatus.FORBIDDEN);
        }

        if (assignedRole == null) {
            assignedRole = Role.STUDENT;
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .enabled(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .accountNonLocked(true)
                .build();

        User savedUser = userRepository.save(user);

        UserProfile.UserProfileBuilder profileBuilder = UserProfile.builder()
                .user(savedUser)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .department(request.getDepartment())
                .enrollmentNumber(request.getEnrollmentNumber());

        if (assignedRole == Role.TEACHER) {
            profileBuilder.teacherVerificationStatus(TeacherVerificationStatus.PENDING);
        }

        UserProfile userProfile = profileBuilder.build();
        userProfileRepository.save(userProfile);

        String accessToken = jwtProvider.generateAccessToken(savedUser);
        String refreshToken = jwtProvider.generateRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(jwtProvider.getAccessTokenExpirationMs())
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();
            UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                    .orElse(UserProfile.builder().firstName("User").lastName("").build());

            String accessToken = jwtProvider.generateAccessToken(user);
            String refreshToken = jwtProvider.generateRefreshToken(user);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresInMs(jwtProvider.getAccessTokenExpirationMs())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .firstName(userProfile.getFirstName())
                    .lastName(userProfile.getLastName())
                    .build();
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new ApiException("User account is deactivated", HttpStatus.UNAUTHORIZED);
        } catch (org.springframework.security.authentication.LockedException e) {
            throw new ApiException("User account is locked", HttpStatus.UNAUTHORIZED);
        } catch (BadCredentialsException e) {
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Authentication error for email {}: {}", request.getEmail(), e.getMessage());
            throw new ApiException("Authentication failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshTokenStr = request.getRefreshToken();

        if (!jwtProvider.validateToken(refreshTokenStr)) {
            throw new ApiException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        String tokenType = jwtProvider.getTokenTypeFromToken(refreshTokenStr);
        if (!JwtProvider.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new ApiException("Provided token is not a valid refresh token", HttpStatus.BAD_REQUEST);
        }

        String email = jwtProvider.getUsernameFromToken(refreshTokenStr);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User associated with refresh token not found", HttpStatus.UNAUTHORIZED));

        if (!user.isEnabled()) {
            throw new ApiException("User account is deactivated", HttpStatus.UNAUTHORIZED);
        }
        if (!user.isAccountNonLocked()) {
            throw new ApiException("User account is locked", HttpStatus.UNAUTHORIZED);
        }

        UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                .orElse(UserProfile.builder().firstName("User").lastName("").build());

        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresInMs(jwtProvider.getAccessTokenExpirationMs())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().toLowerCase().trim()).ifPresent(user -> {
            // Delete prior outstanding tokens for this user
            passwordResetTokenRepository.deleteByUser(user);

            // Generate cryptographically secure 256-bit token
            byte[] randomBytes = new byte[32];
            SECURE_RANDOM.nextBytes(randomBytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

            String tokenHash = hashToken(rawToken);
            Instant expiryDate = Instant.now().plus(24, ChronoUnit.HOURS);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .tokenHash(tokenHash)
                    .user(user)
                    .expiryDate(expiryDate)
                    .consumed(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
            log.info("Dispatched password reset token for user: {}", user.getEmail());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.getToken().trim());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new ApiException("Invalid or expired password reset token", HttpStatus.BAD_REQUEST));

        if (resetToken.isConsumed()) {
            log.warn("Security Alert: Attempt to reuse consumed password reset token ID: {}", resetToken.getId());
            throw new ApiException("Password reset token has already been used", HttpStatus.BAD_REQUEST);
        }

        if (resetToken.isExpired()) {
            log.warn("Password reset attempt with expired token ID: {}", resetToken.getId());
            throw new ApiException("Password reset token has expired", HttpStatus.BAD_REQUEST);
        }

        User user = resetToken.getUser();
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ApiException("New password cannot be identical to the existing password", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setConsumed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
