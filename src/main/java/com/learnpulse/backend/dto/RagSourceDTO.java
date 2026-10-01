package com.learnpulse.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSourceDTO {

    private UUID documentId;
    private Integer chunkIndex;
    private String fileName;
    private String subjectName;
    private String chapterTitle;
    private Double similarityScore;
}
