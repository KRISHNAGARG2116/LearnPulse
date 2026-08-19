package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.ChapterDTO;
import com.learnpulse.backend.dto.CreateChapterRequest;
import com.learnpulse.backend.service.ChapterService;
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
@RequiredArgsConstructor
@Tag(name = "Chapter Management", description = "RESTful APIs for managing academic chapters linked to parent subjects")
public class ChapterController {

    private final ChapterService chapterService;

    @PostMapping("/api/subjects/{subjectId}/chapters")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Create Chapter for Subject", description = "Creates a new chapter linked to a parent subject (Restricted to ADMIN and TEACHER roles)")
    public ResponseEntity<ApiResponse<ChapterDTO>> createChapter(
            @PathVariable UUID subjectId,
            @Valid @RequestBody CreateChapterRequest request) {
        ChapterDTO chapter = chapterService.createChapter(subjectId, request);
        return new ResponseEntity<>(ApiResponse.success("Chapter created successfully", chapter), HttpStatus.CREATED);
    }

    @GetMapping("/api/subjects/{subjectId}/chapters")
    @Operation(summary = "List Chapters for Subject", description = "Retrieves all chapters associated with a parent subject ordered by chapter number")
    public ResponseEntity<ApiResponse<List<ChapterDTO>>> getChaptersBySubject(@PathVariable UUID subjectId) {
        List<ChapterDTO> chapters = chapterService.getChaptersBySubject(subjectId);
        return ResponseEntity.ok(ApiResponse.success("Chapters retrieved successfully", chapters));
    }

    @GetMapping("/api/chapters/{id}")
    @Operation(summary = "Get Chapter by ID", description = "Retrieves details of a specific chapter by its unique ID")
    public ResponseEntity<ApiResponse<ChapterDTO>> getChapterById(@PathVariable UUID id) {
        ChapterDTO chapter = chapterService.getChapterById(id);
        return ResponseEntity.ok(ApiResponse.success("Chapter retrieved successfully", chapter));
    }

    @PutMapping("/api/chapters/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Update Chapter", description = "Updates an existing chapter (Restricted to ADMIN and TEACHER roles)")
    public ResponseEntity<ApiResponse<ChapterDTO>> updateChapter(
            @PathVariable UUID id,
            @Valid @RequestBody CreateChapterRequest request) {
        ChapterDTO chapter = chapterService.updateChapter(id, request);
        return ResponseEntity.ok(ApiResponse.success("Chapter updated successfully", chapter));
    }

    @DeleteMapping("/api/chapters/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Delete Chapter", description = "Deletes an existing chapter (Restricted to ADMIN and TEACHER roles)")
    public ResponseEntity<ApiResponse<Void>> deleteChapter(@PathVariable UUID id) {
        chapterService.deleteChapter(id);
        return ResponseEntity.ok(ApiResponse.success("Chapter deleted successfully", null));
    }
}
