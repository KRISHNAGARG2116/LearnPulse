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
public class FlashcardResponse {

    @Valid
    @NotEmpty(message = "Flashcards list cannot be empty.")
    private List<FlashcardDto> flashcards;
}
