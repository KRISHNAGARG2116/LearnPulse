package com.learnpulse.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevealAnswerRequest {

    @NotBlank(message = "Selected option is required before revealing the correct answer")
    @Pattern(regexp = "^[A-D]$", message = "Selected option must be 'A', 'B', 'C', or 'D'")
    private String selectedOption;
}
