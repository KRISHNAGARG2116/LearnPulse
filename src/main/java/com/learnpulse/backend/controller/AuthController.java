package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.AuthResponse;
import com.learnpulse.backend.dto.LoginRequest;
import com.learnpulse.backend.dto.RefreshTokenRequest;
import com.learnpulse.backend.dto.RegisterRequest;
import com.learnpulse.backend.dto.ForgotPasswordRequest;
import com.learnpulse.backend.dto.ResetPasswordRequest;
import com.learnpulse.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication APIs", description = "Public authentication endpoints for registration, login, and token renewal")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "User Registration", description = "Registers a new user account with BCrypt password hashing and profile creation")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        ApiResponse<AuthResponse> response = ApiResponse.success(
                "User registered successfully",
                authResponse
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates user credentials via Spring Security and issues JWT access & refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        ApiResponse<AuthResponse> response = ApiResponse.success(
                "Authentication successful",
                authResponse
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Validates refresh token and issues a new access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refresh(request);
        ApiResponse<AuthResponse> response = ApiResponse.success(
                "Access token renewed successfully",
                authResponse
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate Password Reset Request", description = "Dispatches a password reset email if the account exists, returning a generic confirmation")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an account with that email exists, password reset instructions have been dispatched.",
                null
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Complete Password Reset", description = "Consumes a valid, unexpired reset token and sets a new account password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password has been reset successfully. Please log in with your new password.",
                null
        ));
    }
}
