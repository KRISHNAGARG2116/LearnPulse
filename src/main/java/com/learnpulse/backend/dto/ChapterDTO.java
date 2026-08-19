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
public class ChapterDTO {

    private UUID id;
    private UUID subjectId;
    private String subjectName;
    private String title;
    private Integer chapterNumber;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
