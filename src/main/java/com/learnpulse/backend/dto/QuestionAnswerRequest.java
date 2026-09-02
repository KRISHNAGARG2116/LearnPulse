package com.learnpulse.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnswerRequest {

    @NotNull(message = "Question ID is required")
    private UUID questionId;

    @NotBlank(message = "Selected answer is required")
    @Pattern(regexp = "^[A-D]$", message = "Selected answer must be 'A', 'B', 'C', or 'D'")
    private String selectedAnswer;
}
