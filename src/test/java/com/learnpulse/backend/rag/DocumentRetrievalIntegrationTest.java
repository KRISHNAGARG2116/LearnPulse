package com.learnpulse.backend.rag;

import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.service.DocumentRetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentRetrievalIntegrationTest {

    @Mock
    private VectorStore vectorStore;

    private DocumentRetrievalService retrievalService;

    @BeforeEach
    public void setUp() {
        retrievalService = new DocumentRetrievalService(vectorStore, 4, 0.5);
    }

    @Test
    @DisplayName("RAG-RETRIEVE-001: Vector Store Similarity Search Execution")
    public void testVectorStoreWriteAndSimilaritySearch() {
        UUID docId = UUID.randomUUID();
        Map<String, Object> meta = new HashMap<>();
        meta.put("documentId", docId.toString());
        meta.put("fileName", "Java_OOP.pdf");
        meta.put("chunkIndex", 0);

        Document matchDoc = new Document("Inheritance allows a child class to inherit features from a parent class.", meta);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.singletonList(matchDoc));

        List<Document> results = retrievalService.retrieveRelevantChunks("What is inheritance?", docId);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(docId.toString(), results.get(0).getMetadata().get("documentId"));
        assertTrue(results.get(0).getContent().contains("Inheritance allows"));
    }

    @Test
    @DisplayName("RAG-RETRIEVE-002: Document ID Filter Expression Enforces Document Isolation")
    public void testDocumentFilterExpression() {
        UUID docId1 = UUID.randomUUID();

        retrievalService.retrieveRelevantChunks("What is polymorphism?", docId1, 4, 0.5);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals("What is polymorphism?", request.getQuery());
        assertEquals(4, request.getTopK());
        assertEquals(0.5, request.getSimilarityThreshold());
        assertTrue(request.hasFilterExpression(), "SearchRequest must include documentId filter expression");
    }

    @Test
    @DisplayName("RAG-RETRIEVE-003: Top-K Configuration Parameter Usage")
    public void testTopKBehavior() {
        UUID docId = UUID.randomUUID();

        retrievalService.retrieveRelevantChunks("Explain exception handling", docId, 2, 0.6);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest request = captor.getValue();
        assertEquals(2, request.getTopK());
        assertEquals(0.6, request.getSimilarityThreshold());
    }

    @Test
    @DisplayName("RAG-RETRIEVE-004: Empty Search Results Handling")
    public void testEmptySearchResults() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        List<Document> results = retrievalService.retrieveRelevantChunks("What is quantum computing?", UUID.randomUUID());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("RAG-RETRIEVE-005: Blank Question Validation Rejection")
    public void testBlankQuestionValidation() {
        assertThrows(ApiException.class, () -> retrievalService.retrieveRelevantChunks("   ", UUID.randomUUID()));
    }
}
