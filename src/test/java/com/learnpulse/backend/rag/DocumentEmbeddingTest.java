package com.learnpulse.backend.rag;

import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.service.DocumentEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DocumentEmbeddingTest {

    @Mock
    private EmbeddingClient embeddingClient;

    private DocumentEmbeddingService embeddingService;

    @BeforeEach
    public void setUp() {
        embeddingService = new DocumentEmbeddingService(embeddingClient, "nomic-embed-text:latest");
    }

    @Test
    @DisplayName("RAG-EMBED-001: Successful Vector Embedding & 768 Dimension Validation")
    public void testSuccessfulQueryEmbedding() {
        List<Double> mockVector = new ArrayList<>();
        for (int i = 0; i < 768; i++) {
            mockVector.add(0.01 * (i + 1));
        }

        when(embeddingClient.embed(anyString())).thenReturn(mockVector);

        List<Double> resultVector = embeddingService.embedQuery("What is inheritance in Java?");

        assertNotNull(resultVector);
        assertEquals(768, resultVector.size());
        assertEquals(0.01, resultVector.get(0));
    }

    @Test
    @DisplayName("RAG-EMBED-002: Empty Query Text Validation")
    public void testEmptyQueryValidation() {
        assertThrows(ApiException.class, () -> embeddingService.embedQuery("   "));
    }

    @Test
    @DisplayName("RAG-EMBED-003: Embedding Client Failure Exception Handling")
    public void testEmbeddingClientFailureHandling() {
        when(embeddingClient.embed(anyString())).thenThrow(new RuntimeException("Ollama connection timed out"));

        ApiException ex = assertThrows(ApiException.class, () -> embeddingService.embedQuery("Explain polymorphism"));
        assertTrue(ex.getMessage().contains("Failed to generate embedding vector"));
    }

    @Test
    @DisplayName("RAG-EMBED-004: Dimension Validation Safety Check")
    public void testDimensionValidation() {
        assertThrows(ApiException.class, () -> embeddingService.validateDimensions(Collections.emptyList()));
        assertThrows(ApiException.class, () -> embeddingService.validateDimensions(null));
    }
}
