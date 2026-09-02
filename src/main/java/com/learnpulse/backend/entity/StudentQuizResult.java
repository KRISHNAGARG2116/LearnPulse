package com.learnpulse.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_quiz_results")
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

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @PrePersist
    protected void onCreate() {
        if (this.attemptedAt == null) {
            this.attemptedAt = Instant.now();
        }
    }
}
