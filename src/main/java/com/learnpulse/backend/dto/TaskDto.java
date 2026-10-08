package com.learnpulse.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {

    @NotBlank(message = "Subject name cannot be blank.")
    private String subject;

    @NotBlank(message = "Topic description cannot be blank.")
    private String topic;

    @NotNull(message = "Task hours cannot be null.")
    @DecimalMin(value = "0.1", message = "Task hours must be greater than 0.")
    private Double hours;
}
