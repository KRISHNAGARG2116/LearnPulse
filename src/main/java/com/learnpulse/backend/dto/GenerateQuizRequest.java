package com.learnpulse.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuizRequest {

    @NotNull(message = "Document ID is required")
    private UUID documentId;

    @Builder.Default
    @Min(value = 1, message = "Question count must be at least 1")
    @Max(value = 20, message = "Question count cannot exceed 20")
    private Integer questionCount = 5;

    private String difficulty;

    private String topic;

    private String quizTitle;

    @Builder.Default
    private Double passingScorePercentage = 80.0;
}
