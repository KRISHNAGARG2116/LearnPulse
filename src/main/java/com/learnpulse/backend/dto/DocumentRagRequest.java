package com.learnpulse.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRagRequest {

    @NotNull(message = "Document ID is required")
    private UUID documentId;

    @NotBlank(message = "Question cannot be empty or whitespace")
    @Size(max = 2000, message = "Question text cannot exceed 2000 characters")
    private String question;
}
