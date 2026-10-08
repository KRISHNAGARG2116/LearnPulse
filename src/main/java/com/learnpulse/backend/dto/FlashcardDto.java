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
public class FlashcardDto {

    @NotBlank(message = "Flashcard question cannot be empty or blank.")
    private String question;

    @NotBlank(message = "Flashcard answer cannot be empty or blank.")
    private String answer;
}
