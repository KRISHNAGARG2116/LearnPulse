package com.learnpulse.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRagResponse {

    private UUID documentId;
    private String question;
    private String answer;
    private List<RagSourceDTO> sources;
    private boolean fallback;
    private Instant timestamp;
}
