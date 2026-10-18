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
public class EnrollmentDTO {

    private UUID id;
    private UUID studentId;
    private String studentEmail;
    private String studentFirstName;
    private String studentLastName;
    private String enrollmentNumber;
    private UUID subjectId;
    private String subjectName;
    private EnrollmentStatus status;
    private Instant enrolledAt;
}
