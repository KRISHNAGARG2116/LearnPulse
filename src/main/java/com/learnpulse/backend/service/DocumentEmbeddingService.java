package com.learnpulse.backend.service;

import com.learnpulse.backend.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class DocumentEmbeddingService {

    public static final int EXPECTED_EMBEDDING_DIMENSIONS = 768;

    private final EmbeddingClient embeddingClient;
    private final String embeddingModelName;

    public DocumentEmbeddingService(
            EmbeddingClient embeddingClient,
            @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text:latest}") String embeddingModelName) {
        this.embeddingClient = embeddingClient;
        this.embeddingModelName = embeddingModelName;
        log.info("Initialized DocumentEmbeddingService using model '{}'", embeddingModelName);
    }

    public List<Double> embedQuery(String queryText) {
        if (!StringUtils.hasText(queryText)) {
            throw new ApiException("Query text cannot be empty for embedding generation", HttpStatus.BAD_REQUEST);
        }

        try {
            List<Double> embedding = embeddingClient.embed(queryText);
            validateDimensions(embedding);
            return embedding;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Embedding generation failed for query using model '{}': {}", embeddingModelName, ex.getMessage(), ex);
            throw new ApiException("Failed to generate embedding vector: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Double> embedDocumentChunk(Document documentChunk) {
        if (documentChunk == null || !StringUtils.hasText(documentChunk.getContent())) {
            return Collections.emptyList();
        }

        try {
            List<Double> embedding = embeddingClient.embed(documentChunk);
            validateDimensions(embedding);
            return embedding;
        } catch (Exception ex) {
            log.error("Failed to generate embedding for document chunk: {}", ex.getMessage(), ex);
            throw new ApiException("Failed to generate document chunk embedding: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<List<Double>> embedTexts(List<String> textList) {
        if (CollectionUtils.isEmpty(textList)) {
            return Collections.emptyList();
        }

        try {
            List<List<Double>> embeddings = embeddingClient.embed(textList);
            for (List<Double> vector : embeddings) {
                validateDimensions(vector);
            }
            return embeddings;
        } catch (Exception ex) {
            log.error("Batch text embedding generation failed using model '{}': {}", embeddingModelName, ex.getMessage(), ex);
            throw new ApiException("Failed to generate batch text embeddings: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void validateDimensions(List<Double> vector) {
        if (CollectionUtils.isEmpty(vector)) {
            throw new ApiException("Embedding client returned an empty or null vector", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (vector.size() != EXPECTED_EMBEDDING_DIMENSIONS) {
            log.warn("Observed embedding vector dimension {} differs from expected {}", vector.size(), EXPECTED_EMBEDDING_DIMENSIONS);
        }
    }

    public String getEmbeddingModelName() {
        return embeddingModelName;
    }
}
