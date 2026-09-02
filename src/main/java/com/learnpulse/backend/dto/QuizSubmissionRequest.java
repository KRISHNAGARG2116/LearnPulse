package com.learnpulse.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmissionRequest {

    @NotNull(message = "Quiz ID is required")
    private UUID quizId;

    @NotEmpty(message = "Submission must contain at least one answer")
    @Valid
    private List<QuestionAnswerRequest> answers;
}
