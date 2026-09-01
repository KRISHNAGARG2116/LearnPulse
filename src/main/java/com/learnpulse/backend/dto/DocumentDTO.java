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
public class DocumentDTO {

    private UUID id;
    private String originalFileName;
    private String storedFileName;
    private Long fileSize;
    private String contentType;

    private UUID teacherId;
    private String teacherEmail;

    private UUID subjectId;
    private String subjectName;

    private UUID chapterId;
    private String chapterTitle;

    private ProcessingStatus processingStatus;
    private String extractedText;
    private Instant createdAt;
}
