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
public class CourseProgressionDTO {

    private UUID courseId;
    private String courseName;
    private String courseCode;
    private Integer totalChapters;
    private Integer completedChapters;
    private Integer currentUnlockedChapterNumber;
    private UUID finalQuizId;
    private String finalQuizTitle;
    private Boolean isFinalQuizUnlocked;
    private Boolean isFinalQuizPassed;
    private Boolean isCourseCompleted;
    private List<ChapterProgressionDTO> chapters;
}
