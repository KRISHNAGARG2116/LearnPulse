package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/enrollment")
@RequiredArgsConstructor
@Tag(name = "Teacher Student Enrollment Management", description = "Endpoints for teachers to manage student enrollments in their assigned subjects")
@SecurityRequirement(name = "bearerAuth")
public class TeacherEnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping("/subjects")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get Assigned Subjects", description = "Retrieves all subjects assigned to the authenticated teacher")
    public ResponseEntity<ApiResponse<List<TeacherSubjectDTO>>> getAssignedSubjects(
            @AuthenticationPrincipal User teacher) {

        List<TeacherSubjectDTO> subjects = enrollmentService.getTeacherSubjects(teacher);
        return ResponseEntity.ok(ApiResponse.success("Assigned subjects retrieved successfully", subjects));
    }

    @GetMapping("/subjects/{subjectId}/students")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get Enrolled Students for Subject", description = "Retrieves enrolled students for a teacher-assigned subject")
    public ResponseEntity<ApiResponse<List<StudentSummaryDTO>>> getEnrolledStudents(
            @AuthenticationPrincipal User teacher,
            @PathVariable UUID subjectId) {

        List<StudentSummaryDTO> students = enrollmentService.getStudentsForSubject(teacher, subjectId);
        return ResponseEntity.ok(ApiResponse.success("Enrolled students retrieved successfully", students));
    }

    @PostMapping("/subjects/{subjectId}/students")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Enroll Student in Subject", description = "Enrolls a student in a teacher-assigned subject")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> enrollStudent(
            @AuthenticationPrincipal User teacher,
            @PathVariable UUID subjectId,
            @Valid @RequestBody EnrollStudentRequest request) {

        EnrollmentDTO enrollment = enrollmentService.enrollStudentInSubject(teacher, subjectId, request.getStudentId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student enrolled successfully", enrollment));
    }

    @DeleteMapping("/subjects/{subjectId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Unenroll Student from Subject", description = "Soft-unenrolls a student from a subject (preserves historical attempts/grades)")
    public ResponseEntity<ApiResponse<Void>> unenrollStudent(
            @AuthenticationPrincipal User teacher,
            @PathVariable UUID subjectId,
            @PathVariable UUID studentId) {

        enrollmentService.unenrollStudentFromSubject(teacher, subjectId, studentId);
        return ResponseEntity.ok(ApiResponse.success("Student unenrolled successfully (historical results preserved)", null));
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "List All Students (Safe Summary)", description = "Retrieves safe student summaries for the enrollment picker (no passwords/tokens)")
    public ResponseEntity<ApiResponse<List<StudentSummaryDTO>>> getAllStudents(
            @AuthenticationPrincipal User teacher) {

        List<StudentSummaryDTO> students = enrollmentService.getAllStudentsForTeacher(teacher);
        return ResponseEntity.ok(ApiResponse.success("Students retrieved successfully", students));
    }
}
