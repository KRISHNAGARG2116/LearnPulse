package com.learnpulse.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDTO {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private int chapterCount;
    private Instant createdAt;
    private Instant updatedAt;
}
