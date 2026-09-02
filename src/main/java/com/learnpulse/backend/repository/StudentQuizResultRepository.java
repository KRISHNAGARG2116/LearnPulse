package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.StudentQuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentQuizResultRepository extends JpaRepository<StudentQuizResult, UUID> {

    List<StudentQuizResult> findByStudentIdOrderByAttemptedAtDesc(UUID studentId);

    List<StudentQuizResult> findByStudentIdAndQuizIdOrderByAttemptedAtDesc(UUID studentId, UUID quizId);

    List<StudentQuizResult> findByQuizId(UUID quizId);
}
