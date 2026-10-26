package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.TeacherVerificationStatus;
import com.learnpulse.backend.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    List<UserProfile> findByTeacherVerificationStatus(TeacherVerificationStatus status);

    List<UserProfile> findByUserRoleAndTeacherVerificationStatus(Role role, TeacherVerificationStatus status);
}
