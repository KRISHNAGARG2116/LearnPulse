package com.learnpulse.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevealAnswerResponseDTO {

    private UUID questionId;
    private String selectedOption;

    @JsonProperty("isCorrect")
    private boolean isCorrect;

    private String correctAnswer;
    private String explanation;
}
