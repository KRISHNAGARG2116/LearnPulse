package com.learnpulse.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
public class CreateQuizRequest {

    @NotBlank(message = "Quiz title is required")
    @Size(min = 3, max = 200, message = "Quiz title must be between 3 and 200 characters")
    private String title;

    private String description;
    private UUID subjectId;
    private UUID chapterId;

    @NotEmpty(message = "Quiz must contain at least one question")
    @Valid
    private List<CreateQuestionRequest> questions;
}
