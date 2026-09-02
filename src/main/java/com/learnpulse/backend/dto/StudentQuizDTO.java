package com.learnpulse.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuizDTO {

    private UUID id;
    private String title;
    private String description;
    private UUID subjectId;
    private String subjectName;
    private UUID chapterId;
    private String chapterTitle;
    private Integer totalMarks;
    private UUID createdById;
    private String createdByEmail;
    private List<StudentQuestionDTO> questions;
    private Instant createdAt;
}
