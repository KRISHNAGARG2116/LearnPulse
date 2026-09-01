package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.Notes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotesRepository extends JpaRepository<Notes, UUID> {

    List<Notes> findByTeacherId(UUID teacherId);

    List<Notes> findBySubjectId(UUID subjectId);

    List<Notes> findByChapterId(UUID chapterId);
}
