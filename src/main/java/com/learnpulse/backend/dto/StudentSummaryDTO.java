package com.learnpulse.backend.dto;

import com.learnpulse.backend.entity.EnrollmentStatus;
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
public class StudentSummaryDTO {

    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String department;
    private String enrollmentNumber;
    private Instant enrolledAt;
    private EnrollmentStatus status;
}
