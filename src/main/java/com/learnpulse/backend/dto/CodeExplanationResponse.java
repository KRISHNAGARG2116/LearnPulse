package com.learnpulse.backend.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CodeExplanationResponse {

    @NotBlank(message = "Purpose description cannot be blank.")
    private String purpose;

    @NotEmpty(message = "Step-by-step logic list cannot be empty.")
    private List<String> stepByStepLogic;

    private List<String> variables;
    private List<String> methods;

    @NotBlank(message = "Control flow description cannot be blank.")
    private String controlFlow;

    @NotBlank(message = "Algorithm description cannot be blank.")
    private String algorithm;

    @NotBlank(message = "Time complexity cannot be blank.")
    private String timeComplexity;

    @NotBlank(message = "Space complexity cannot be blank.")
    private String spaceComplexity;

    private List<String> potentialImprovements;
}
