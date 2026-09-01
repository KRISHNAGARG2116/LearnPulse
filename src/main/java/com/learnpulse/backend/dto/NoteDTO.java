package com.learnpulse.backend.dto;

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
public class NoteDTO {

    private UUID id;
    private String title;
    private String content;

    private UUID teacherId;
    private String teacherEmail;

    private UUID subjectId;
    private String subjectName;

    private UUID chapterId;
    private String chapterTitle;

    private UUID documentId;
    private String documentFileName;
    private Instant createdAt;
}
