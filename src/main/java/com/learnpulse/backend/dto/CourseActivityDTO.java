package com.learnpulse.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseActivityDTO {
    private UUID subjectId;
    private String subjectName;
    private String subjectCode;
    private long enrolledStudentsCount;
    private long chaptersCount;
    private long quizzesCount;
    private long documentsCount;
}
