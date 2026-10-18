package com.learnpulse.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuizAiResponse {

    private String title;

    @NotEmpty(message = "Generated questions list cannot be empty")
    @Valid
    private List<GeneratedQuizQuestionDto> questions;
}
