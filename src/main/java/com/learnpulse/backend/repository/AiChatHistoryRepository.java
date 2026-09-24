package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.AiChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiChatHistoryRepository extends JpaRepository<AiChatHistory, UUID> {

    List<AiChatHistory> findByUserIdAndConversationIdOrderByCreatedAtAsc(UUID userId, String conversationId);

    List<AiChatHistory> findTop10ByUserIdAndConversationIdOrderByCreatedAtDesc(UUID userId, String conversationId);

    List<AiChatHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
