package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.AssignTeacherRequest;
import com.learnpulse.backend.dto.TeacherProfileDTO;
import com.learnpulse.backend.dto.TeacherSubjectDTO;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/subjects")
@RequiredArgsConstructor
@Tag(name = "Admin Teacher-Subject Association", description = "Administrative endpoints to assign, remove, and list teachers for academic subjects")
@SecurityRequirement(name = "bearerAuth")
public class AdminEnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/{subjectId}/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign Teacher to Subject", description = "Assigns a teacher to an academic subject (Admin only)")
    public ResponseEntity<ApiResponse<TeacherSubjectDTO>> assignTeacher(
            @AuthenticationPrincipal User admin,
            @PathVariable UUID subjectId,
            @Valid @RequestBody AssignTeacherRequest request) {

        TeacherSubjectDTO dto = enrollmentService.assignTeacherToSubject(admin, subjectId, request.getTeacherId());
        return ResponseEntity.ok(ApiResponse.success("Teacher assigned to subject successfully", dto));
    }

    @DeleteMapping("/{subjectId}/teachers/{teacherId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove Teacher from Subject", description = "Removes a teacher assignment from an academic subject (Admin only)")
    public ResponseEntity<ApiResponse<Void>> removeTeacher(
            @AuthenticationPrincipal User admin,
            @PathVariable UUID subjectId,
            @PathVariable UUID teacherId) {

        enrollmentService.removeTeacherFromSubject(admin, subjectId, teacherId);
        return ResponseEntity.ok(ApiResponse.success("Teacher removed from subject successfully", null));
    }

    @GetMapping("/{subjectId}/teachers")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Teachers for Subject", description = "Retrieves all teachers assigned to a specific subject")
    public ResponseEntity<ApiResponse<List<TeacherProfileDTO>>> getSubjectTeachers(@PathVariable UUID subjectId) {
        List<TeacherProfileDTO> teachers = enrollmentService.getSubjectTeachers(subjectId);
        return ResponseEntity.ok(ApiResponse.success("Subject teachers retrieved successfully", teachers));
    }
}
