package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findBySubjectId(UUID subjectId);

    List<Quiz> findByChapterId(UUID chapterId);

    List<Quiz> findBySubjectIdAndChapterId(UUID subjectId, UUID chapterId);
}
