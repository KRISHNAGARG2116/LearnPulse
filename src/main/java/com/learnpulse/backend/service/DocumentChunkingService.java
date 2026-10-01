package com.learnpulse.backend.service;

import com.learnpulse.backend.entity.UploadedDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Slf4j
public class DocumentChunkingService {

    private final int chunkSize;
    private final int chunkOverlap;
    private final int minChunkSize;
    private final TokenTextSplitter textSplitter;

    public DocumentChunkingService(
            @Value("${rag.chunk-size:800}") int chunkSize,
            @Value("${rag.chunk-overlap:100}") int chunkOverlap,
            @Value("${rag.min-chunk-size:20}") int minChunkSize) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.minChunkSize = minChunkSize;
        // TokenTextSplitter(defaultChunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator)
        this.textSplitter = new TokenTextSplitter(chunkSize, minChunkSize, minChunkSize, 10000, true);
        log.info("Initialized DocumentChunkingService with chunkSize={}, chunkOverlap={}, minChunkSize={}",
                chunkSize, chunkOverlap, minChunkSize);
    }

    public List<Document> chunkDocument(UploadedDocument document) {
        if (document == null) {
            log.warn("Cannot chunk null document");
            return Collections.emptyList();
        }

        String extractedText = document.getExtractedText();
        if (!StringUtils.hasText(extractedText)) {
            log.warn("Document ID {} has empty or blank extracted text; skipping chunking", document.getId());
            return Collections.emptyList();
        }

        Map<String, Object> baseMetadata = buildMetadataMap(document);
        Document rawDocument = new Document(extractedText, baseMetadata);

        List<Document> rawChunks = textSplitter.apply(Collections.singletonList(rawDocument));

        List<Document> finalChunks = new ArrayList<>();
        int chunkIndex = 0;

        for (Document chunk : rawChunks) {
            String content = chunk.getContent();
            if (!StringUtils.hasText(content) || content.trim().length() < minChunkSize) {
                continue;
            }

            Map<String, Object> chunkMetadata = new HashMap<>(chunk.getMetadata());
            chunkMetadata.put("chunkIndex", chunkIndex);
            chunkMetadata.put("documentId", document.getId().toString());

            Document processedChunk = new Document(chunk.getId(), content.trim(), chunkMetadata);
            finalChunks.add(processedChunk);
            chunkIndex++;
        }

        log.info("Chunked document '{}' (ID: {}) into {} chunks",
                document.getOriginalFileName(), document.getId(), finalChunks.size());

        return finalChunks;
    }

    public List<Document> chunkText(String text, Map<String, Object> metadata) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }

        Map<String, Object> baseMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        Document rawDocument = new Document(text, baseMetadata);

        List<Document> rawChunks = textSplitter.apply(Collections.singletonList(rawDocument));
        List<Document> finalChunks = new ArrayList<>();
        int chunkIndex = 0;

        for (Document chunk : rawChunks) {
            String content = chunk.getContent();
            if (!StringUtils.hasText(content) || content.trim().length() < minChunkSize) {
                continue;
            }

            Map<String, Object> chunkMetadata = new HashMap<>(chunk.getMetadata());
            chunkMetadata.put("chunkIndex", chunkIndex);

            Document processedChunk = new Document(chunk.getId(), content.trim(), chunkMetadata);
            finalChunks.add(processedChunk);
            chunkIndex++;
        }

        return finalChunks;
    }

    private Map<String, Object> buildMetadataMap(UploadedDocument document) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", document.getId().toString());
        metadata.put("fileName", document.getOriginalFileName());
        metadata.put("source", document.getOriginalFileName());
        metadata.put("contentType", document.getContentType() != null ? document.getContentType() : "application/pdf");
        metadata.put("fileSize", document.getFileSize() != null ? document.getFileSize() : 0L);

        if (document.getSubject() != null) {
            metadata.put("subjectId", document.getSubject().getId().toString());
            metadata.put("subjectName", document.getSubject().getName());
        } else {
            metadata.put("subjectId", "");
            metadata.put("subjectName", "");
        }

        if (document.getChapter() != null) {
            metadata.put("chapterId", document.getChapter().getId().toString());
            metadata.put("chapterTitle", document.getChapter().getTitle());
        } else {
            metadata.put("chapterId", "");
            metadata.put("chapterTitle", "");
        }

        if (document.getTeacher() != null) {
            metadata.put("uploadedBy", document.getTeacher().getEmail());
        } else {
            metadata.put("uploadedBy", "");
        }

        return metadata;
    }
}
