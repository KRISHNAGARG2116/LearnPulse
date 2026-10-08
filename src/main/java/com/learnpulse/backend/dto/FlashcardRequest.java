package com.learnpulse.backend.dto;

import com.learnpulse.backend.config.AiLimitsConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardRequest {

    @NotBlank(message = "Educational content is required and cannot be empty or blank.")
    @Size(max = AiLimitsConstants.MAX_FLASHCARDS_CHARS, message = "Educational content exceeds maximum allowed length of 15,000 characters.")
    private String content;

    @Min(value = 1, message = "Card count must be at least 1.")
    @Max(value = 20, message = "Card count cannot exceed 20.")
    private Integer cardCount;
}
