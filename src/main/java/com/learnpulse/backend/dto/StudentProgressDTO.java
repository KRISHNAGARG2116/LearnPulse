package com.learnpulse.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProgressDTO {

    private UUID studentId;
    private String studentEmail;

    private Integer totalQuizzesAttempted;
    private Integer totalQuizzesCompleted;

    private Double averageScore;
    private Double averagePercentage;
    private Integer highestScore;

    private Integer totalCorrectAnswers;
    private Integer totalWrongAnswers;

    private List<StudentQuizResultDTO> recentAttempts;
}
