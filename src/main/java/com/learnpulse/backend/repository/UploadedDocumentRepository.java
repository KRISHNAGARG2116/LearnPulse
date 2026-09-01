package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, UUID> {

    List<UploadedDocument> findByTeacherIdAndActiveTrue(UUID teacherId);

    List<UploadedDocument> findBySubjectIdAndActiveTrue(UUID subjectId);

    List<UploadedDocument> findByChapterIdAndActiveTrue(UUID chapterId);

    Optional<UploadedDocument> findByIdAndActiveTrue(UUID id);
}
