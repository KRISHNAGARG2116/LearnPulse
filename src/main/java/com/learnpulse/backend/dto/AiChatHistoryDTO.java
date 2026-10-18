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
public class AiChatHistoryDTO {

    private UUID id;
    private String conversationId;
    private String userMessage;
    private String aiResponse;
    private Instant createdAt;
}
