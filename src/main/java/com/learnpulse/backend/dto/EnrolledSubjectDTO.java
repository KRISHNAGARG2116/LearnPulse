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
public class EnrolledSubjectDTO {

    private UUID subjectId;
    private String subjectName;
    private String subjectCode;
    private String subjectDescription;
    private int chapterCount;
    private EnrollmentStatus status;
    private Instant enrolledAt;
    private String teacherFirstName;
    private String teacherLastName;
    private String teacherEmail;
}
