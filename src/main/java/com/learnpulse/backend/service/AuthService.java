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
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;

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

        UserProfile userProfile = UserProfile.builder()
                .user(savedUser)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .department(request.getDepartment())
                .enrollmentNumber(request.getEnrollmentNumber())
                .build();

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
}
