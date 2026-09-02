package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.CreateQuizRequest;
import com.learnpulse.backend.dto.TeacherQuizDTO;
import com.learnpulse.backend.entity.User;
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

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher Quiz Management", description = "Endpoints for Teachers and Admins to create and manage assessment quizzes")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TeacherQuizController {

    private final QuizService quizService;

    @PostMapping("/create-quiz")
    @Operation(summary = "Create a new quiz with nested questions", description = "Creates a quiz and persists all nested questions in a single atomic transaction")
    public ResponseEntity<ApiResponse<TeacherQuizDTO>> createQuiz(
            @AuthenticationPrincipal User creator,
            @Valid @RequestBody CreateQuizRequest request) {

        TeacherQuizDTO createdQuiz = quizService.createQuiz(creator, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quiz created successfully", createdQuiz));
    }
}
