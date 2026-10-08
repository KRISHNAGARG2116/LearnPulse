package com.learnpulse.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learnpulse.backend.config.AiLimitsConstants;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlanRequest {

    @NotNull(message = "Exam date is required.")
    @Future(message = "Exam date must be in the future.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate examDate;

    @NotEmpty(message = "At least one subject is required.")
    @Size(max = AiLimitsConstants.MAX_STUDY_PLAN_SUBJECTS, message = "Subjects list cannot exceed 10 items.")
    private List<String> subjects;

    @NotNull(message = "Available study hours per day is required.")
    @Min(value = 1, message = "Study hours per day must be at least 1 hour.")
    @Max(value = 16, message = "Study hours per day cannot exceed 16 hours.")
    private Double availableHoursPerDay;
}
