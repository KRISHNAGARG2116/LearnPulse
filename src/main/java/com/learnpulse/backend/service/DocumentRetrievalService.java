package com.learnpulse.backend.service;

import com.learnpulse.backend.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DocumentRetrievalService {

    private final VectorStore vectorStore;
    private final int defaultTopK;
    private final double defaultSimilarityThreshold;

    public DocumentRetrievalService(
            VectorStore vectorStore,
            @Value("${rag.top-k:4}") int defaultTopK,
            @Value("${rag.similarity-threshold:0.5}") double defaultSimilarityThreshold) {
        this.vectorStore = vectorStore;
        this.defaultTopK = defaultTopK;
        this.defaultSimilarityThreshold = defaultSimilarityThreshold;
        log.info("Initialized DocumentRetrievalService with topK={}, similarityThreshold={}",
                defaultTopK, defaultSimilarityThreshold);
    }

    public List<Document> retrieveRelevantChunks(String question, UUID documentId) {
        return retrieveRelevantChunks(question, documentId, defaultTopK, defaultSimilarityThreshold);
    }

    public List<Document> retrieveRelevantChunks(String question, UUID documentId, int topK, double similarityThreshold) {
        if (!StringUtils.hasText(question)) {
            throw new ApiException("Question cannot be empty or blank", HttpStatus.BAD_REQUEST);
        }

        try {
            SearchRequest searchRequest = SearchRequest.query(question.trim())
                    .withTopK(topK > 0 ? topK : defaultTopK)
                    .withSimilarityThreshold(similarityThreshold >= 0.0 ? similarityThreshold : defaultSimilarityThreshold);

            if (documentId != null) {
                FilterExpressionBuilder builder = new FilterExpressionBuilder();
                searchRequest = searchRequest.withFilterExpression(builder.eq("documentId", documentId.toString()).build());
            }

            log.info("Executing vector similarity search for question: '{}' (documentId filter: {}, topK: {}, threshold: {})",
                    question, documentId, searchRequest.getTopK(), searchRequest.getSimilarityThreshold());

            List<Document> matchedDocuments = vectorStore.similaritySearch(searchRequest);

            if (matchedDocuments == null) {
                matchedDocuments = Collections.emptyList();
            }

            log.info("Vector similarity search returned {} matching chunks for question: '{}'", matchedDocuments.size(), question);
            return matchedDocuments;

        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Vector retrieval failed for query '{}': {}", question, ex.getMessage(), ex);
            throw new ApiException("Vector search failure: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public double getDefaultSimilarityThreshold() {
        return defaultSimilarityThreshold;
    }
}
