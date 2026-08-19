package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.TeacherProfileDTO;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher/profile")
@RequiredArgsConstructor
@Tag(name = "Teacher Profile Management", description = "Endpoints for retrieving and updating authenticated teacher profiles")
public class TeacherProfileController {

    private final TeacherService teacherService;

    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get Authenticated Teacher Profile", description = "Retrieves profile of the currently authenticated teacher")
    public ResponseEntity<ApiResponse<TeacherProfileDTO>> getProfile(@AuthenticationPrincipal User currentUser) {
        TeacherProfileDTO profile = teacherService.getTeacherProfile(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Teacher profile retrieved successfully", profile));
    }

    @PutMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Update Authenticated Teacher Profile", description = "Updates profile of the currently authenticated teacher (enforces SecurityContext ownership)")
    public ResponseEntity<ApiResponse<TeacherProfileDTO>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody TeacherProfileDTO dto) {
        TeacherProfileDTO updatedProfile = teacherService.updateTeacherProfile(currentUser, dto);
        return ResponseEntity.ok(ApiResponse.success("Teacher profile updated successfully", updatedProfile));
    }
}
