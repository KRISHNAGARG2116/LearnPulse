package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

    List<Chapter> findBySubjectIdOrderByChapterNumberAsc(UUID subjectId);

    boolean existsBySubjectIdAndChapterNumber(UUID subjectId, Integer chapterNumber);
}
