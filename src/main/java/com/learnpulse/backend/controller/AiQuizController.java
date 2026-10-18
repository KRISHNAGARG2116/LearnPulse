package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.GenerateQuizRequest;
import com.learnpulse.backend.dto.GeneratedQuizResponse;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.AiQuizGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Quiz Generator", description = "Endpoints for Teachers and Admins to generate and persist quizzes from course material using AI")
@SecurityRequirement(name = "bearerAuth")
public class AiQuizController {

    private final AiQuizGenerationService aiQuizGenerationService;

    @PostMapping("/generate-quiz")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(
            summary = "Generate and Persist AI Quiz",
            description = "Analyzes educational course document text, generates validated MCQs with explanations, and persists the quiz in DRAFT status for teacher review."
    )
    public ResponseEntity<ApiResponse<GeneratedQuizResponse>> generateQuiz(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody GenerateQuizRequest request) {

        log.info("Received POST /api/ai/generate-quiz request from user {} for document ID {}",
                currentUser != null ? currentUser.getEmail() : "anonymous",
                request != null ? request.getDocumentId() : null);

        GeneratedQuizResponse response = aiQuizGenerationService.generateAndPersistQuiz(currentUser, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("AI Quiz generated and saved in draft status successfully", response));
    }
}
