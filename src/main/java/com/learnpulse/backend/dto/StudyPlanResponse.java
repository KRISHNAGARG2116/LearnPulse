package com.learnpulse.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class StudyPlanResponse {

    @NotNull(message = "Exam date cannot be null.")
    private LocalDate examDate;

    @Min(value = 1, message = "Days remaining must be at least 1.")
    private long daysRemaining;

    @NotNull(message = "Available study hours per day cannot be null.")
    private Double availableHoursPerDay;

    @Valid
    @NotEmpty(message = "Daily study plan schedule cannot be empty.")
    private List<DailyScheduleDto> plan;
}
