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
public class CodeExplanationRequest {

    @NotBlank(message = "Source code is required and cannot be empty or blank.")
    @Size(max = AiLimitsConstants.MAX_CODE_EXPLAIN_CHARS, message = "Source code exceeds maximum allowed length of 10,000 characters.")
    private String code;

    private String language;
}
