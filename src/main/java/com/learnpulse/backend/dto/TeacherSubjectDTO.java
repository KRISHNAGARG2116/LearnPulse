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
public class TeacherSubjectDTO {

    private UUID subjectId;
    private String subjectName;
    private String subjectCode;
    private String subjectDescription;
    private int chapterCount;
    private Instant assignedAt;
}
