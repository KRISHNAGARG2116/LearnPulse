package com.learnpulse.backend.service;

import com.learnpulse.backend.ai.PromptManager;
import com.learnpulse.backend.dto.DocumentRagRequest;
import com.learnpulse.backend.dto.DocumentRagResponse;
import com.learnpulse.backend.dto.RagSourceDTO;
import com.learnpulse.backend.entity.UploadedDocument;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.UploadedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentRagService {

    public static final String NO_CONTEXT_FOUND_RESPONSE = "The requested information could not be found in the selected document.";
    public static final String LLM_FALLBACK_RESPONSE = "The AI service is currently unavailable or offline. Please try again shortly.";

    private final UploadedDocumentRepository documentRepository;
    private final DocumentChunkingService chunkingService;
    private final VectorStore vectorStore;
    private final DocumentRetrievalService retrievalService;
    private final PromptManager promptManager;
    private final ChatClient chatClient;

    private final Set<UUID> indexedDocumentCache = ConcurrentHashMap.newKeySet();

    public DocumentRagResponse askDocument(User studentUser, DocumentRagRequest request) {
        if (studentUser == null) {
            throw new ApiException("Authentication required to ask document questions", HttpStatus.UNAUTHORIZED);
        }

        if (request == null || request.getDocumentId() == null) {
            throw new ApiException("Document ID is required", HttpStatus.BAD_REQUEST);
        }

        String question = request.getQuestion();
        if (!StringUtils.hasText(question)) {
            throw new ApiException("Question cannot be empty or whitespace", HttpStatus.BAD_REQUEST);
        }

        UUID documentId = request.getDocumentId();

        // 1. Validate Document Existence and Authorization
        UploadedDocument document = documentRepository.findByIdAndActiveTrue(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        log.info("Student {} ({}) querying document '{}' (ID: {}) with question: '{}'",
                studentUser.getEmail(), studentUser.getRole(), document.getOriginalFileName(), documentId, question);

        // 2. Ensure Document is Indexed into PgVectorStore (Idempotent Ingestion)
        ensureDocumentIndexed(document);

        // 3. Perform Vector Similarity Retrieval
        List<Document> retrievedChunks = retrievalService.retrieveRelevantChunks(
                question,
                documentId,
                retrievalService.getDefaultTopK(),
                retrievalService.getDefaultSimilarityThreshold()
        );

        // 4. Handle No-Context / Below-Threshold Scenario (NO CONTEXT -> NO FAKE ANSWER)
        if (CollectionUtils.isEmpty(retrievedChunks)) {
            log.info("No relevant context found for question '{}' on document ID {}", question, documentId);
            return DocumentRagResponse.builder()
                    .documentId(documentId)
                    .question(question)
                    .answer(NO_CONTEXT_FOUND_RESPONSE)
                    .sources(Collections.emptyList())
                    .fallback(false)
                    .timestamp(Instant.now())
                    .build();
        }

        // 5. Construct Grounded RAG Context & Prompt
        String combinedContextText = buildCombinedContextText(retrievedChunks);
        Prompt ragPrompt = promptManager.buildRagPrompt(combinedContextText, question);

        // 6. Execute LLM Call using Existing Qwen ChatClient
        String aiAnswer;
        boolean fallback = false;
        try {
            log.info("Dispatching RAG prompt with {} retrieved chunks to ChatClient/Ollama", retrievedChunks.size());
            aiAnswer = chatClient.call(ragPrompt).getResult().getOutput().getContent();

            if (!StringUtils.hasText(aiAnswer)) {
                aiAnswer = NO_CONTEXT_FOUND_RESPONSE;
            }
        } catch (Exception ex) {
            log.error("LLM execution failed during RAG request for document ID {}: {}", documentId, ex.getMessage(), ex);
            aiAnswer = LLM_FALLBACK_RESPONSE;
            fallback = true;
        }

        // 7. Map Source Metadata
        List<RagSourceDTO> sources = mapToSourceDTOs(retrievedChunks);

        return DocumentRagResponse.builder()
                .documentId(documentId)
                .question(question)
                .answer(aiAnswer)
                .sources(sources)
                .fallback(fallback)
                .timestamp(Instant.now())
                .build();
    }

    public synchronized void ensureDocumentIndexed(UploadedDocument document) {
        if (document == null || document.getId() == null) {
            return;
        }

        UUID docId = document.getId();
        if (indexedDocumentCache.contains(docId)) {
            return;
        }

        List<Document> existingVectorStoreChunks = retrievalService.retrieveRelevantChunks("test search", docId, 1, 0.0);
        if (!CollectionUtils.isEmpty(existingVectorStoreChunks)) {
            log.info("Document ID {} is already indexed in PgVectorStore", docId);
            indexedDocumentCache.add(docId);
            return;
        }

        log.info("Indexing document ID {} ('{}') into PgVectorStore...", docId, document.getOriginalFileName());
        List<Document> chunks = chunkingService.chunkDocument(document);

        if (!CollectionUtils.isEmpty(chunks)) {
            vectorStore.add(chunks);
            log.info("Successfully stored {} vector chunks for document ID {} in PgVectorStore", chunks.size(), docId);
        } else {
            log.warn("No valid chunks produced for document ID {}", docId);
        }

        indexedDocumentCache.add(docId);
    }

    private String buildCombinedContextText(List<Document> chunks) {
        StringBuilder sb = new StringBuilder();
        int chunkNum = 1;

        for (Document chunk : chunks) {
            sb.append("--- [Chunk ").append(chunkNum).append("] ---\n");
            sb.append(chunk.getContent()).append("\n\n");
            chunkNum++;
        }

        return sb.toString().trim();
    }

    private List<RagSourceDTO> mapToSourceDTOs(List<Document> chunks) {
        if (CollectionUtils.isEmpty(chunks)) {
            return Collections.emptyList();
        }

        return chunks.stream().map(chunk -> {
            Map<String, Object> meta = chunk.getMetadata();
            UUID docId = null;
            if (meta.containsKey("documentId")) {
                try {
                    docId = UUID.fromString(meta.get("documentId").toString());
                } catch (Exception ignored) {}
            }

            Integer chunkIdx = null;
            if (meta.containsKey("chunkIndex")) {
                try {
                    chunkIdx = Integer.parseInt(meta.get("chunkIndex").toString());
                } catch (Exception ignored) {}
            }

            String fileName = meta.getOrDefault("fileName", meta.getOrDefault("source", "")).toString();
            String subjectName = meta.getOrDefault("subjectName", "").toString();
            String chapterTitle = meta.getOrDefault("chapterTitle", "").toString();
            Double score = null;
            if (meta.containsKey("distance")) {
                try {
                    score = Double.parseDouble(meta.get("distance").toString());
                } catch (Exception ignored) {}
            }

            return RagSourceDTO.builder()
                    .documentId(docId)
                    .chunkIndex(chunkIdx)
                    .fileName(fileName)
                    .subjectName(subjectName)
                    .chapterTitle(chapterTitle)
                    .similarityScore(score)
                    .build();
        }).collect(Collectors.toList());
    }
}
