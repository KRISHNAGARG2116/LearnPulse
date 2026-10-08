package com.learnpulse.backend.dto;

import com.learnpulse.backend.config.AiLimitsConstants;
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
public class SummarizeRequest {

    @NotBlank(message = "Document content is required and cannot be empty or blank.")
    @Size(max = AiLimitsConstants.MAX_SUMMARIZE_CHARS, message = "Document content exceeds maximum allowed length of 20,000 characters.")
    private String content;
}
