package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.CodeExplanationService;
import com.learnpulse.backend.service.DocumentSummarizationService;
import com.learnpulse.backend.service.FlashcardGenerationService;
import com.learnpulse.backend.service.StudyPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "Supplemental AI Learning Features", description = "Endpoints for document summarization, code explanation, study planning, and flashcards")
public class AiFeaturesController {

    private final DocumentSummarizationService summarizationService;
    private final CodeExplanationService codeExplanationService;
    private final StudyPlanService studyPlanService;
    private final FlashcardGenerationService flashcardService;

    @PostMapping("/summarize")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Summarize Document Text", description = "Generates a structured summary, key takeaways, and keywords from provided text.")
    public ResponseEntity<ApiResponse<SummaryResponse>> summarize(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SummarizeRequest request) {
        
        log.info("Received POST /api/ai/summarize request from student: {}", currentUser.getEmail());
        SummaryResponse response = summarizationService.summarizeDocument(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Document summarized successfully", response));
    }

    @PostMapping("/explain-code")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Explain Source Code Statically", description = "Generates a structured static analysis and explanation of source code.")
    public ResponseEntity<ApiResponse<CodeExplanationResponse>> explainCode(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CodeExplanationRequest request) {

        log.info("Received POST /api/ai/explain-code request from student: {}", currentUser.getEmail());
        CodeExplanationResponse response = codeExplanationService.explainCode(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Code explained successfully", response));
    }

    @PostMapping("/study-plan")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Generate AI Study Plan", description = "Generates a daily study schedule bounded by available study hours per day.")
    public ResponseEntity<ApiResponse<StudyPlanResponse>> generateStudyPlan(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody StudyPlanRequest request) {

        log.info("Received POST /api/ai/study-plan request from student: {}", currentUser.getEmail());
        StudyPlanResponse response = studyPlanService.generateStudyPlan(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Study plan generated successfully", response));
    }

    @PostMapping("/flashcards")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Generate AI Flashcards", description = "Generates question and answer flashcards from educational material.")
    public ResponseEntity<ApiResponse<FlashcardResponse>> generateFlashcards(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody FlashcardRequest request) {

        log.info("Received POST /api/ai/flashcards request from student: {}", currentUser.getEmail());
        FlashcardResponse response = flashcardService.generateFlashcards(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Flashcards generated successfully", response));
    }
}
