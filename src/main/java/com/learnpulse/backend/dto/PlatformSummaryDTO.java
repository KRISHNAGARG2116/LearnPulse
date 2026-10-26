package com.learnpulse.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSummaryDTO {
    // User metrics
    private long totalUsers;
    private long activeUsers;
    private long deactivatedUsers;
    private long studentsCount;
    private long teachersCount;
    private long adminsCount;

    // Course & Academic metrics
    private long totalSubjects;
    private long totalChapters;
    private long totalDocuments;
    private long activeDocuments;

    // Assessment metrics
    private long totalQuizzes;
    private long publishedQuizzes;
    private long draftQuizzes;
    private long completedQuizAttempts;
    private long passedQuizAttempts;
    private double averageQuizScore;

    // Enrollment metrics
    private long totalActiveEnrollments;
}
