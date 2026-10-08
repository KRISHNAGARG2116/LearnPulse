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
public class SummaryResponse {

    @NotBlank(message = "Summary text cannot be empty or null.")
    private String summary;

    @NotEmpty(message = "Key takeaways list cannot be empty or null.")
    private List<String> keyTakeaways;

    @NotEmpty(message = "Keywords list cannot be empty or null.")
    private List<String> keywords;
}
