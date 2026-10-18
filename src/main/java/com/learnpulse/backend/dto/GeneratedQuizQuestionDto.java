package com.learnpulse.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuizQuestionDto {

    @NotBlank(message = "Question text cannot be blank")
    private String questionText;

    @NotBlank(message = "Option A cannot be blank")
    private String optionA;

    @NotBlank(message = "Option B cannot be blank")
    private String optionB;

    @NotBlank(message = "Option C cannot be blank")
    private String optionC;

    @NotBlank(message = "Option D cannot be blank")
    private String optionD;

    @NotBlank(message = "Correct answer must be specified (A, B, C, or D)")
    private String correctAnswer;

    @NotBlank(message = "Explanation cannot be blank")
    private String explanation;

    private String difficulty;

    @Builder.Default
    private Integer marks = 1;
}
