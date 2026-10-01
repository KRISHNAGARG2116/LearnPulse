package com.learnpulse.backend.rag;

import com.learnpulse.backend.entity.Chapter;
import com.learnpulse.backend.entity.Subject;
import com.learnpulse.backend.entity.UploadedDocument;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.DocumentChunkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentChunkingTest {

    private DocumentChunkingService chunkingService;

    @BeforeEach
    public void setUp() {
        chunkingService = new DocumentChunkingService(800, 100, 20);
    }

    @Test
    @DisplayName("RAG-CHUNK-001: Normal Document Text Chunking & Metadata Preservation")
    public void testChunkingNormalDocument() {
        UUID docId = UUID.randomUUID();
        User teacher = User.builder().email("teacher@learnpulse.ai").build();
        Subject subject = Subject.builder().id(UUID.randomUUID()).name("Java Programming").build();
        Chapter chapter = Chapter.builder().id(UUID.randomUUID()).title("Object Oriented Fundamentals").build();

        UploadedDocument document = UploadedDocument.builder()
                .id(docId)
                .originalFileName("Java_Fundamentals.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .teacher(teacher)
                .subject(subject)
                .chapter(chapter)
                .extractedText("Inheritance is a mechanism in Java where a new class inherits attributes and methods from an existing class. Polymorphism allows methods to do different things based on the object.")
                .build();

        List<Document> chunks = chunkingService.chunkDocument(document);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertEquals(1, chunks.size());

        Document firstChunk = chunks.get(0);
        assertTrue(firstChunk.getContent().contains("Inheritance is a mechanism"));
        assertEquals(docId.toString(), firstChunk.getMetadata().get("documentId"));
        assertEquals("Java_Fundamentals.pdf", firstChunk.getMetadata().get("fileName"));
        assertEquals(0, firstChunk.getMetadata().get("chunkIndex"));
        assertEquals("Java Programming", firstChunk.getMetadata().get("subjectName"));
        assertEquals("Object Oriented Fundamentals", firstChunk.getMetadata().get("chapterTitle"));
        assertEquals("teacher@learnpulse.ai", firstChunk.getMetadata().get("uploadedBy"));
    }

    @Test
    @DisplayName("RAG-CHUNK-002: Large Content Multi-Chunk Generation & Index Order")
    public void testChunkingMultipleChunks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 200; i++) {
            sb.append("Section ").append(i).append(": Java supports object-oriented programming concepts such as encapsulation, inheritance, polymorphism, and abstraction. ");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", "Large_Course_Material.pdf");

        List<Document> chunks = chunkingService.chunkText(sb.toString(), metadata);

        assertNotNull(chunks);
        assertTrue(chunks.size() > 1, "Large text should produce multiple chunks");

        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            assertEquals(i, chunk.getMetadata().get("chunkIndex"), "Chunk index must be deterministic and ordered");
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().trim().isEmpty());
        }
    }

    @Test
    @DisplayName("RAG-CHUNK-003: Empty and Blank Text Chunking Safety")
    public void testNullAndEmptyContent() {
        List<Document> nullChunks = chunkingService.chunkText(null, null);
        assertNotNull(nullChunks);
        assertTrue(nullChunks.isEmpty());

        List<Document> blankChunks = chunkingService.chunkText("   \n\t  ", null);
        assertNotNull(blankChunks);
        assertTrue(blankChunks.isEmpty());

        UploadedDocument emptyDoc = UploadedDocument.builder()
                .id(UUID.randomUUID())
                .originalFileName("Empty.pdf")
                .extractedText("")
                .build();

        List<Document> emptyDocChunks = chunkingService.chunkDocument(emptyDoc);
        assertNotNull(emptyDocChunks);
        assertTrue(emptyDocChunks.isEmpty());
    }

    @Test
    @DisplayName("RAG-CHUNK-004: Minimum Chunk Size Filtering")
    public void testSmallContentHandling() {
        List<Document> tinyChunks = chunkingService.chunkText("Hi", null);
        assertNotNull(tinyChunks);
        assertTrue(tinyChunks.isEmpty(), "Content smaller than minChunkSize should be ignored");
    }

    @Test
    @DisplayName("RAG-CHUNK-005: Custom Metadata Field Preservation")
    public void testCustomMetadataPreservation() {
        Map<String, Object> customMeta = new HashMap<>();
        customMeta.put("documentId", "doc-12345");
        customMeta.put("source", "Java_Spec.pdf");
        customMeta.put("courseId", "course-99");

        List<Document> chunks = chunkingService.chunkText("Java interfaces define a contract that implementing classes must follow.", customMeta);

        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        Document chunk = chunks.get(0);
        assertEquals("doc-12345", chunk.getMetadata().get("documentId"));
        assertEquals("Java_Spec.pdf", chunk.getMetadata().get("source"));
        assertEquals("course-99", chunk.getMetadata().get("courseId"));
        assertEquals(0, chunk.getMetadata().get("chunkIndex"));
    }
}
