package com.learnpulse.backend.dto;

import jakarta.validation.Valid;
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
public class DailyScheduleDto {

    @NotBlank(message = "Date cannot be blank.")
    private String date;

    @Valid
    @NotEmpty(message = "Daily tasks list cannot be empty.")
    private List<TaskDto> tasks;
}
