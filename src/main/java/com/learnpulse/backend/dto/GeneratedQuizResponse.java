package com.learnpulse.backend.dto;

import com.learnpulse.backend.entity.QuizStatus;
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
public class GeneratedQuizResponse {

    private UUID quizId;
    private String title;
    private String description;
    private UUID subjectId;
    private String subjectName;
    private UUID chapterId;
    private String chapterTitle;
    private UUID documentId;
    private String documentFileName;
    private QuizStatus status;
    @com.fasterxml.jackson.annotation.JsonProperty("isPublished")
    private Boolean isPublished;
    private Double passingScorePercentage;
    private Integer totalMarks;
    private Integer questionsCount;
    private List<TeacherQuestionDTO> questions;
    private Instant createdAt;
}
