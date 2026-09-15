package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.CourseProgressionDTO;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.CourseProgressionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/student/courses")
@RequiredArgsConstructor
@Tag(name = "Student Course Progression APIs", description = "Endpoints for retrieving student course and chapter progression state")
public class StudentProgressionController {

    private final CourseProgressionService courseProgressionService;

    @GetMapping("/{subjectId}/progression")
    @Operation(summary = "Get Student Course Progression", description = "Retrieves authoritative course progression, completed chapters, unlocked status, and final quiz availability for the authenticated student")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourseProgressionDTO>> getCourseProgression(
            @AuthenticationPrincipal User student,
            @PathVariable UUID subjectId) {

        CourseProgressionDTO progression = courseProgressionService.getCourseProgression(student, subjectId);
        return ResponseEntity.ok(ApiResponse.success("Course progression retrieved successfully", progression));
    }
}
