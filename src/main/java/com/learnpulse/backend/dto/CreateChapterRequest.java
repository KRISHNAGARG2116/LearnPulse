package com.learnpulse.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChapterRequest {

    @NotBlank(message = "Chapter title is required")
    @Size(min = 2, max = 150, message = "Chapter title must be between 2 and 150 characters")
    private String title;

    @NotNull(message = "Chapter number is required")
    @Min(value = 1, message = "Chapter number must be at least 1")
    private Integer chapterNumber;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
