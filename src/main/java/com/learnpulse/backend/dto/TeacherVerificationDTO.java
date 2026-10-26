package com.learnpulse.backend.dto;

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
public class TeacherVerificationDTO {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String department;
    private TeacherVerificationStatus status;
    private String rejectionReason;
    private Instant registeredAt;
}
