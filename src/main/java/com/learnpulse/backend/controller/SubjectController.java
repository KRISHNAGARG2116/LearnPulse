package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.CreateSubjectRequest;
import com.learnpulse.backend.dto.SubjectDTO;
import com.learnpulse.backend.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Tag(name = "Subject Management", description = "RESTful APIs for creating, reading, updating, and deleting academic subjects")
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Create Academic Subject", description = "Creates a new subject (Restricted to ADMIN and TEACHER roles)")
    public ResponseEntity<ApiResponse<SubjectDTO>> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        SubjectDTO subject = subjectService.createSubject(request);
        return new ResponseEntity<>(ApiResponse.success("Subject created successfully", subject), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List All Subjects", description = "Retrieves a list of all academic subjects")
    public ResponseEntity<ApiResponse<List<SubjectDTO>>> getAllSubjects() {
        List<SubjectDTO> subjects = subjectService.getAllSubjects();
        return ResponseEntity.ok(ApiResponse.success("Subjects retrieved successfully", subjects));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Subject by ID", description = "Retrieves details of a specific subject by its unique ID")
    public ResponseEntity<ApiResponse<SubjectDTO>> getSubjectById(@PathVariable UUID id) {
        SubjectDTO subject = subjectService.getSubjectById(id);
        return ResponseEntity.ok(ApiResponse.success("Subject retrieved successfully", subject));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Update Subject", description = "Updates an existing subject (Restricted to ADMIN and TEACHER roles)")
    public ResponseEntity<ApiResponse<SubjectDTO>> updateSubject(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSubjectRequest request) {
        SubjectDTO subject = subjectService.updateSubject(id, request);
        return ResponseEntity.ok(ApiResponse.success("Subject updated successfully", subject));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Delete Subject", description = "Deletes an existing subject and its child chapters (Restricted to ADMIN and TEACHER roles)")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable UUID id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok(ApiResponse.success("Subject deleted successfully", null));
    }
}
