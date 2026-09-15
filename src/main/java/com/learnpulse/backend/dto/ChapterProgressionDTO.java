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
public class ChapterProgressionDTO {

    private UUID chapterId;
    private String title;
    private Integer chapterNumber;
    private String description;
    private Boolean isUnlocked;
    private Boolean isCompleted;
    private UUID quizId;
    private String quizTitle;
    private Double highestScorePercentage;
    private Boolean isQuizPassed;
}
