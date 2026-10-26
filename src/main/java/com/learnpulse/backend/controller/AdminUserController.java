package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User & Role Governance", description = "Privileged endpoints for user directory, roles, lifecycle deactivation, and password reset")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List Users", description = "Paginated directory of system users with role and status filtering")
    public ResponseEntity<ApiResponse<Page<AdminUserDTO>>> listUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AdminUserDTO> users = adminUserService.getUsers(role, enabled, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get User Details", description = "Detailed administrative user profile with activity metrics")
    public ResponseEntity<ApiResponse<AdminUserDetailDTO>> getUserDetail(@PathVariable UUID id) {
        AdminUserDetailDTO detail = adminUserService.getUserDetail(id);
        return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", detail));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update User Profile", description = "Updates administrative profile fields for a user")
    public ResponseEntity<ApiResponse<AdminUserDTO>> updateUserProfile(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserProfileRequest request) {

        AdminUserDTO updated = adminUserService.updateUserProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", updated));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update User Role", description = "Modifies a user's role with self-demotion and last-admin safeguards")
    public ResponseEntity<ApiResponse<AdminUserDTO>> updateUserRole(
            @AuthenticationPrincipal User actingAdmin,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request) {

        AdminUserDTO updated = adminUserService.updateUserRole(actingAdmin, id, request.getRole());
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", updated));
    }

    @PostMapping("/{id}/password-reset")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Initiate Password Reset", description = "Generates a time-limited password reset token without exposing credentials")
    public ResponseEntity<ApiResponse<PasswordResetResponse>> initiatePasswordReset(
            @AuthenticationPrincipal User actingAdmin,
            @PathVariable UUID id) {

        PasswordResetResponse response = adminUserService.initiatePasswordReset(actingAdmin, id);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate User", description = "Deactivates an account with immediate token invalidation")
    public ResponseEntity<ApiResponse<AdminUserDTO>> deactivateUser(
            @AuthenticationPrincipal User actingAdmin,
            @PathVariable UUID id) {

        AdminUserDTO updated = adminUserService.deactivateUser(actingAdmin, id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", updated));
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate User", description = "Re-enables a previously deactivated account")
    public ResponseEntity<ApiResponse<AdminUserDTO>> reactivateUser(
            @AuthenticationPrincipal User actingAdmin,
            @PathVariable UUID id) {

        AdminUserDTO updated = adminUserService.reactivateUser(actingAdmin, id);
        return ResponseEntity.ok(ApiResponse.success("User reactivated successfully", updated));
    }
}
