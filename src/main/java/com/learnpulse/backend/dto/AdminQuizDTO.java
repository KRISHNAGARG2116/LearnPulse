package com.learnpulse.backend.dto;

import com.learnpulse.backend.entity.QuizStatus;
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
public class AdminQuizDTO {
    private UUID id;
    private String title;
    private String subjectName;
    private QuizStatus status;
    private Boolean isPublished;
    private Integer totalMarks;
    private String creatorEmail;
    private Instant createdAt;
}
