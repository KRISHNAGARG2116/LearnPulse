package com.learnpulse.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_quiz_results", indexes = {
        // Week 14 — Performance: supports student progress history queries (ordered by time)
        @Index(name = "idx_quiz_result_student_time", columnList = "student_id, attempted_at"),
        // Week 14 — Performance: supports per-quiz result aggregation in admin analytics
        @Index(name = "idx_quiz_result_quiz_id", columnList = "quiz_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks;

    @Column(name = "percentage", nullable = false)
    private Double percentage;

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers;

    @Column(name = "wrong_answers", nullable = false)
    private Integer wrongAnswers;

    @Builder.Default
    @Column(name = "is_passed", nullable = false)
    private Boolean isPassed = false;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @PrePersist
    protected void onCreate() {
        if (this.attemptedAt == null) {
            this.attemptedAt = Instant.now();
        }
    }
}
