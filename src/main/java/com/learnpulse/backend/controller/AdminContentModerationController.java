package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.QuizStatus;
import com.learnpulse.backend.entity.TeacherVerificationStatus;
import com.learnpulse.backend.service.AdminContentModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Content Moderation & Teacher Governance", description = "Endpoints for faculty verification, uploaded documents moderation, and quiz publication governance")
@SecurityRequirement(name = "bearerAuth")
public class AdminContentModerationController {

    private final AdminContentModerationService moderationService;

    // --- Teacher Verification ---

    @GetMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List Teachers", description = "Retrieves all teacher profiles with verification status")
    public ResponseEntity<ApiResponse<List<TeacherVerificationDTO>>> listTeachers(
            @RequestParam(required = false) TeacherVerificationStatus status) {

        List<TeacherVerificationDTO> teachers = moderationService.getTeachers(status);
        return ResponseEntity.ok(ApiResponse.success("Teachers retrieved successfully", teachers));
    }

    @GetMapping("/teachers/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List Pending Teacher Verifications", description = "Retrieves faculty members awaiting administrative credential verification")
    public ResponseEntity<ApiResponse<List<TeacherVerificationDTO>>> listPendingTeachers() {
        List<TeacherVerificationDTO> pending = moderationService.getTeachers(TeacherVerificationStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.success("Pending teachers retrieved successfully", pending));
    }

    @PostMapping("/teachers/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve Teacher Verification", description = "Approves a faculty member's credentials and unlocks teaching privileges")
    public ResponseEntity<ApiResponse<TeacherVerificationDTO>> approveTeacher(@PathVariable UUID id) {
        TeacherVerificationDTO approved = moderationService.approveTeacher(id);
        return ResponseEntity.ok(ApiResponse.success("Teacher verification approved successfully", approved));
    }

    @PostMapping("/teachers/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject Teacher Verification", description = "Rejects a faculty member's verification with recorded rationale")
    public ResponseEntity<ApiResponse<TeacherVerificationDTO>> rejectTeacher(
            @PathVariable UUID id,
            @RequestBody(required = false) RejectTeacherRequest request) {

        String reason = (request != null) ? request.getReason() : null;
        TeacherVerificationDTO rejected = moderationService.rejectTeacher(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Teacher verification rejected", rejected));
    }

    // --- Content Moderation: Documents ---

    @GetMapping("/content/documents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List Uploaded Documents for Moderation", description = "Retrieves all curriculum documents with active status")
    public ResponseEntity<ApiResponse<List<AdminDocumentDTO>>> listDocuments(
            @RequestParam(required = false) Boolean active) {

        List<AdminDocumentDTO> docs = moderationService.getDocuments(active);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", docs));
    }

    @PatchMapping("/content/documents/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle Document Active Status", description = "Activates or deactivates an uploaded curriculum document")
    public ResponseEntity<ApiResponse<AdminDocumentDTO>> updateDocumentStatus(
            @PathVariable UUID id,
            @RequestBody UpdateContentStatusRequest request) {

        Boolean active = (request != null) ? request.getActive() : null;
        AdminDocumentDTO updated = moderationService.updateDocumentStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success("Document status updated successfully", updated));
    }

    // --- Content Moderation: Quizzes ---

    @GetMapping("/content/quizzes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List Quizzes for Moderation", description = "Retrieves all quizzes with draft/published state")
    public ResponseEntity<ApiResponse<List<AdminQuizDTO>>> listQuizzes(
            @RequestParam(required = false) QuizStatus status) {

        List<AdminQuizDTO> quizzes = moderationService.getQuizzes(status);
        return ResponseEntity.ok(ApiResponse.success("Quizzes retrieved successfully", quizzes));
    }

    @PatchMapping("/content/quizzes/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Quiz Moderation Status", description = "Modifies a quiz's publication status")
    public ResponseEntity<ApiResponse<AdminQuizDTO>> updateQuizStatus(
            @PathVariable UUID id,
            @RequestBody UpdateContentStatusRequest request) {

        QuizStatus status = null;
        if (request != null && request.getStatus() != null) {
            try {
                status = QuizStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        Boolean isPublished = (request != null) ? request.getActive() : null;

        AdminQuizDTO updated = moderationService.updateQuizStatus(id, status, isPublished);
        return ResponseEntity.ok(ApiResponse.success("Quiz status updated successfully", updated));
    }
}
