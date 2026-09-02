package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.QuizGradingService;
import com.learnpulse.backend.service.QuizService;
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
@RequiredArgsConstructor
@Tag(name = "Student Quiz & Assessment Engine", description = "Endpoints for students to discover quizzes, attempt tests, view automatic grading results, and track progress")
@SecurityRequirement(name = "bearerAuth")
public class StudentQuizController {

    private final QuizService quizService;
    private final QuizGradingService quizGradingService;

    @GetMapping("/api/quizzes")
    @Operation(summary = "List available quizzes", description = "Retrieves available quizzes, optionally filtered by subject ID and/or chapter ID. Correct answers are strictly hidden.")
    public ResponseEntity<ApiResponse<List<StudentQuizDTO>>> getQuizzes(
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) UUID chapterId) {

        List<StudentQuizDTO> quizzes = quizService.getQuizzesForStudent(subjectId, chapterId);
        return ResponseEntity.ok(ApiResponse.success("Quizzes retrieved successfully", quizzes));
    }

    @GetMapping("/api/quizzes/{id}")
    @Operation(summary = "Get quiz details for delivery", description = "Retrieves details and questions for a specific quiz. Correct answers are strictly hidden.")
    public ResponseEntity<ApiResponse<StudentQuizDTO>> getQuizById(@PathVariable UUID id) {
        StudentQuizDTO quiz = quizService.getQuizByIdForStudent(id);
        return ResponseEntity.ok(ApiResponse.success("Quiz retrieved successfully", quiz));
    }

    @PostMapping("/api/quizzes/{quizId}/questions/{questionId}/reveal")
    @Operation(summary = "Controlled single-question answer reveal", description = "Reveals the correct answer for a specific question ONLY after validating that an option has been selected by the student.")
    public ResponseEntity<ApiResponse<RevealAnswerResponseDTO>> revealAnswer(
            @AuthenticationPrincipal User student,
            @PathVariable UUID quizId,
            @PathVariable UUID questionId,
            @Valid @RequestBody RevealAnswerRequest request) {

        RevealAnswerResponseDTO reveal = quizService.revealAnswerForQuestion(student, quizId, questionId, request);
        return ResponseEntity.ok(ApiResponse.success("Answer revealed successfully", reveal));
    }

    @PostMapping("/api/quizzes/submit")
    @Operation(summary = "Submit quiz answers for automatic grading", description = "Grades student answer selections server-side, calculates percentage and score, and persists attempt result")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<StudentQuizResultDTO>> submitQuiz(
            @AuthenticationPrincipal User student,
            @Valid @RequestBody QuizSubmissionRequest submission) {

        StudentQuizResultDTO result = quizGradingService.submitQuiz(student, submission);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quiz submitted and graded successfully", result));
    }

    @GetMapping("/api/quizzes/result/{id}")
    @Operation(summary = "Get quiz attempt result", description = "Retrieves specific quiz result by ID. Enforces ownership: students can only access their own results.")
    public ResponseEntity<ApiResponse<StudentQuizResultDTO>> getResultById(
            @AuthenticationPrincipal User requester,
            @PathVariable UUID id) {

        StudentQuizResultDTO result = quizGradingService.getResultById(requester, id);
        return ResponseEntity.ok(ApiResponse.success("Quiz result retrieved successfully", result));
    }

    @GetMapping("/api/student/progress")
    @Operation(summary = "Get student performance analytics and progress", description = "Calculates total quizzes attempted, completion count, average score, percentage, and recent attempt history")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentProgressDTO>> getStudentProgress(
            @AuthenticationPrincipal User student) {

        StudentProgressDTO progress = quizGradingService.getStudentProgress(student);
        return ResponseEntity.ok(ApiResponse.success("Student progress retrieved successfully", progress));
    }
}
