package com.learnpulse.backend.dto;

import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.TeacherVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailDTO {
    private UUID id;
    private String email;
    private Role role;
    private boolean enabled;
    private boolean accountNonLocked;
    private Instant createdAt;
    private Instant updatedAt;
    private String firstName;
    private String lastName;
    private String department;
    private String enrollmentNumber;
    private TeacherVerificationStatus teacherVerificationStatus;
    private String rejectionReason;

    // Activity metrics
    private long enrolledSubjectsCount;
    private long assignedSubjectsCount;
    private long completedQuizzesCount;
    private long createdQuizzesCount;
    private long uploadedDocumentsCount;
}
