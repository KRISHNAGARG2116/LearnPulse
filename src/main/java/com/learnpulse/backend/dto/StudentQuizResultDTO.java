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
public class StudentQuizResultDTO {

    private UUID id;
    private UUID quizId;
    private String quizTitle;

    private UUID studentId;
    private String studentEmail;

    private Integer score;
    private Integer totalMarks;
    private Double percentage;

    private Integer correctAnswers;
    private Integer wrongAnswers;

    private Instant attemptedAt;
}
