package com.learnpulse.backend.dto;

import com.learnpulse.backend.entity.ProcessingStatus;
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
public class AdminDocumentDTO {
    private UUID id;
    private String originalFileName;
    private Long fileSize;
    private String contentType;
    private String teacherEmail;
    private String subjectName;
    private boolean active;
    private ProcessingStatus processingStatus;
    private Instant createdAt;
}
