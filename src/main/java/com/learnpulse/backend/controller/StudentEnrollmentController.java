package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.EnrolledSubjectDTO;
import com.learnpulse.backend.dto.EnrollmentDTO;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/enrollment")
@RequiredArgsConstructor
@Tag(name = "Student Enrollment APIs", description = "Endpoints for students to discover their enrolled subjects and courses")
@SecurityRequirement(name = "bearerAuth")
public class StudentEnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping("/subjects")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get Enrolled Subjects", description = "Retrieves only the subjects in which the student is actively enrolled, along with teacher details")
    public ResponseEntity<ApiResponse<List<EnrolledSubjectDTO>>> getEnrolledSubjects(
            @AuthenticationPrincipal User student) {

        List<EnrolledSubjectDTO> subjects = enrollmentService.getEnrolledSubjectsForStudent(student);
        return ResponseEntity.ok(ApiResponse.success("Enrolled subjects retrieved successfully", subjects));
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get My Enrollments", description = "Retrieves all enrollment records for the authenticated student")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getMyEnrollments(
            @AuthenticationPrincipal User student) {

        List<EnrollmentDTO> enrollments = enrollmentService.getMyEnrollments(student);
        return ResponseEntity.ok(ApiResponse.success("Enrollments retrieved successfully", enrollments));
    }
}
