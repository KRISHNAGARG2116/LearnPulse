package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.EnrollmentStatus;
import com.learnpulse.backend.entity.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {

    List<StudentEnrollment> findByStudentId(UUID studentId);

    List<StudentEnrollment> findByStudentIdAndStatus(UUID studentId, EnrollmentStatus status);

    List<StudentEnrollment> findBySubjectId(UUID subjectId);

    List<StudentEnrollment> findBySubjectIdAndStatus(UUID subjectId, EnrollmentStatus status);

    Optional<StudentEnrollment> findByStudentIdAndSubjectId(UUID studentId, UUID subjectId);

    boolean existsByStudentIdAndSubjectId(UUID studentId, UUID subjectId);

    boolean existsByStudentIdAndSubjectIdAndStatus(UUID studentId, UUID subjectId, EnrollmentStatus status);
}
