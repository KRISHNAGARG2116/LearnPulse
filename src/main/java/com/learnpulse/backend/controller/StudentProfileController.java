package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.StudentProfileDTO;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/profile")
@RequiredArgsConstructor
@Tag(name = "Student Profile Management", description = "Endpoints for retrieving and updating authenticated student profiles")
public class StudentProfileController {

    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get Authenticated Student Profile", description = "Retrieves profile of the currently authenticated student")
    public ResponseEntity<ApiResponse<StudentProfileDTO>> getProfile(@AuthenticationPrincipal User currentUser) {
        StudentProfileDTO profile = studentService.getStudentProfile(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Student profile retrieved successfully", profile));
    }

    @PutMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Update Authenticated Student Profile", description = "Updates profile of the currently authenticated student (enforces SecurityContext ownership)")
    public ResponseEntity<ApiResponse<StudentProfileDTO>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody StudentProfileDTO dto) {
        StudentProfileDTO updatedProfile = studentService.updateStudentProfile(currentUser, dto);
        return ResponseEntity.ok(ApiResponse.success("Student profile updated successfully", updatedProfile));
    }
}
