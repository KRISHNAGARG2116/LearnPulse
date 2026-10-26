package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, UUID> {

    List<TeacherSubject> findByTeacherId(UUID teacherId);

    List<TeacherSubject> findBySubjectId(UUID subjectId);

    Optional<TeacherSubject> findByTeacherIdAndSubjectId(UUID teacherId, UUID subjectId);

    boolean existsByTeacherIdAndSubjectId(UUID teacherId, UUID subjectId);

    long countByTeacherId(UUID teacherId);
}
