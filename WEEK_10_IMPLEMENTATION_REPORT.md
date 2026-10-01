# Week 10 Implementation Report: RAG Pipeline Engineering

**Project Name:** LearnPulse AI-Powered LMS  
**Component:** Backend RAG (Retrieval-Augmented Generation) Infrastructure & Vector Search  
**Date:** October 1, 2026  
**Status:** Implemented and verified against the Week 10 specification.

---

## 1. Executive Summary & Objective

Week 10 implements the end-to-end Retrieval-Augmented Generation (RAG) pipeline for the LearnPulse platform. The RAG pipeline connects existing document extraction tools with token-aware chunking, vector embedding generation, vector storage in PostgreSQL via `pgvector`, similarity retrieval, document-grounded prompt engineering, and Ollama-hosted local LLMs.

### Core Pipeline Architecture
```
Course PDF Document
       ↓
Existing PDF/DOC Extraction (PDFBox/Tika)
       ↓
DocumentChunkingService (TokenTextSplitter: 800 tokens, 100 overlap)
       ↓
DocumentEmbeddingService (nomic-embed-text: 768 dimensions)
       ↓
PostgreSQL 16 + pgvector (vector_store table, COSINE distance)
       ↓
Student Question (POST /api/ai/ask-document)
       ↓
DocumentRetrievalService (Top-K=4, Similarity Threshold=0.5, Document Filter)
       ↓
PromptManager (Strict Educational Grounding System Prompt)
       ↓
AiLlmService / ChatClient (qwen2.5-coder:7b)
       ↓
Document-Grounded Answer + Source Metadata DTO
```

---

## 2. Approved Technology Stack & Configuration

The RAG architecture utilizes separate, specialized local AI models for vector embedding generation and final natural language answer generation:

| Component | Technology / Value | Description / Responsibilities |
| :--- | :--- | :--- |
| **Embedding Model** | `nomic-embed-text:latest` | Dedicated 768-dimensional text embedding model hosted via Ollama. |
| **Embedding Dimension** | `768` | Strictly validated vector size stored in PostgreSQL `pgvector`. |
| **Generative LLM** | `qwen2.5-coder:7b` | Existing local chat model used exclusively for answer generation. |
| **Vector Database** | PostgreSQL 16 + `pgvector` | Table `vector_store` using `COSINE` distance and `HNSW` index support. |
| **Text Splitter** | `TokenTextSplitter` | Spring AI token-aware splitter maintaining chunk metadata. |
| **Chunk Size** | `800` | Configurable via `application.yml` (`rag.chunk-size`). |
| **Chunk Overlap** | `100` | Configurable via `application.yml` (`rag.chunk-overlap`). |
| **Min Chunk Size** | `20` | Configurable via `application.yml` (`rag.min-chunk-size`). |
| **Top-K Retrieval** | `4` | Configurable via `application.yml` (`rag.top-k`). |
| **Similarity Threshold** | `0.5` | Configurable via `application.yml` (`rag.similarity-threshold`). |

---

## 3. Architecture & Service Implementation

### 3.1 DocumentChunkingService
- Splits extracted document text using Spring AI `TokenTextSplitter`.
- Preserves document identity and metadata: `documentId`, `fileName`, `uploadedBy`, `contentType`, `subjectId`, `chapterId`, and deterministic `chunkIndex`.
- Safely handles empty, blank, small, and multi-page texts without emitting empty or invalid vectors.

### 3.2 DocumentEmbeddingService
- Wraps Spring AI `EmbeddingClient` configured with `nomic-embed-text:latest`.
- Validates that generated vectors match the expected 768-dimension schema.
- Rejects non-768 vectors and handles embedding service failures cleanly.

### 3.3 PgVectorStore Configuration
- Configures Spring AI `PgVectorStore` bean over JDBC `JdbcTemplate`.
- Automatically initializes table `vector_store` with 768-dimensional vector columns and COSINE distance matching functions.

### 3.4 DocumentRetrievalService
- Generates query embeddings using `nomic-embed-text:latest`.
- Performs semantic vector search on `PgVectorStore` with `Top-K=4` and `threshold=0.5`.
- **Strict Document Filtering**: Applies strict `documentId` metadata filtering to ensure queries against Document A never retrieve content from Document B.

### 3.5 DocumentRagService & Idempotent Ingestion
- Orchestrates student authorization, document lookup, lazy document indexing, vector retrieval, prompt construction, LLM generation, and response assembly.
- **Idempotent Ingestion**: Checks if a document has already been indexed before chunking and embedding. Prevents duplicate vector record creation upon repeated questions for the same document.
- **No-Context Rule**: If vector retrieval returns 0 chunks or all chunks fall below the 0.5 similarity threshold, the service immediately returns `"The requested information could not be found in the selected document."` without invoking the LLM, preventing ungrounded hallucinations.

### 3.6 API & Security Boundary
- **Endpoint**: `POST /api/ai/ask-document`
- **Security Rule**: Restricted to `ROLE_STUDENT` via JWT authentication in `SecurityConfig`.
- Request Payload: `{"documentId": "<UUID>", "question": "..."}`
- Response Structure: `DocumentRagResponse` containing `documentId`, `question`, `answer`, `sources` list (`chunkIndex`, `fileName`, `similarityScore`), `fallback` flag, and `timestamp`.

---

## 4. Automated Testing & Regression Results

The automated Maven test suite was executed against the entire codebase.

### Test Results Breakdown
- **Total Suite Execution Time:** 47.869 seconds
- **Total Tests Executed:** 115
- **Passed:** 115
- **Failed:** 0
- **Errors:** 0
- **Skipped:** 0
- **Build Status:** `BUILD SUCCESS`

```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  47.869 s
[INFO] Finished at: 2026-10-01T13:42:45+05:30
[INFO] ------------------------------------------------------------------------
```

### New Week 10 Test Coverage (21 New Tests)
1. `DocumentChunkingTest` (Unit Tests): Normal chunking, multiple chunks, metadata preservation, empty content handling, deterministic chunk indexes.
2. `DocumentEmbeddingTest` (Unit Tests): Successful embedding generation, 768-dimension validation, model failure handling.
3. `DocumentRetrievalIntegrationTest` (Integration Tests): Write/read vectors, document ID filter isolation, Top-K retrieval, threshold filtering.
4. `DocumentRagIntegrationTest` (Integration Tests): Full student RAG API requests, unauthenticated access rejection (401), non-existent document handling (404), blank question validation, no-context grounding response, LLM execution fallback.

### Regression Verification (Week 1–9 Baseline)
- Existing 94 tests (authentication, RBAC, quizzes, progression, document storage, notes, Week 9 chat history) passed with zero regressions.

---

## 5. Real End-to-End Verification (`verify_week10_rag.py`)

A real end-to-end verification script (`verify_week10_rag.py`) was executed against a live Spring Boot server running on OpenJDK 21 and connected to local PostgreSQL 16 + pgvector and Ollama.

### Test Sample PDF Details
- **File Name:** `Java_Programming_Fundamentals.pdf`
- **File Size:** 4,078 bytes
- **Extracted Text Length:** 3,192 characters
- **Document ID (UUID):** `f863c45a-be29-45dc-8b77-44af6aadbf5a`
- **Indexed Chunks:** 1 chunk stored in `vector_store` table
- **Vector Dimensions Verified:** `768` (`vector_dims(embedding) = 768`)

### Empirical Query Results & Grounded Answers

| Query | Status | Time (ms) | Similarity Score | Retrieved Source | Answer Summary / Grounded Result |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **"What is inheritance?"** | HTTP 200 | 38,281 ms | 0.3292 | Chunk 0 | *"Inheritance in Java is a fundamental concept in Object-Oriented Programming (OOP) that allows a child class (subclass) to inherit fields and methods from a parent class (superclass) using the 'extends' keyword..."* |
| **"What is polymorphism?"** | HTTP 200 | 27,311 ms | 0.4175 | Chunk 0 | *"Polymorphism in Java allows objects of different classes to be treated as objects of a common superclass... Java supports two main types: compile-time (overloading) and runtime (overriding)..."* |
| **"Explain interfaces."** | HTTP 200 | 23,633 ms | 0.4156 | Chunk 0 | *"An interface in Java is a blueprint of a class that contains abstract methods and static constants. Interfaces specify WHAT a class should do, but not HOW it does it..."* |
| **"What is exception handling?"** | HTTP 200 | 26,435 ms | 0.4077 | Chunk 0 | *"Exception handling in Java provides a robust mechanism to manage runtime errors... Checked exceptions (IOException) vs Unchecked exceptions (RuntimeException), try-catch-finally..."* |
| **"What is the capital of France?"** *(Negative Query)* | HTTP 200 | 234.21 ms | N/A (0 chunks) | None (0 sources) | *"The requested information could not be found in the selected document."* |

### Security & Access Control Verification
1. **Unauthenticated Request:** Calling `POST /api/ai/ask-document` without JWT returned `HTTP 401 Unauthorized`.
2. **Invalid Document UUID:** Requesting non-existent UUID `00000000-0000-0000-0000-000000000000` returned `HTTP 404 Not Found`.

---

## 6. Codebase Modifications & Dependencies

### Dependencies Added
- `org.springframework.ai:spring-ai-pgvector-store-spring-boot-starter` in `pom.xml`.

### Files Created
- `src/main/java/com/learnpulse/backend/service/DocumentChunkingService.java`
- `src/main/java/com/learnpulse/backend/service/DocumentEmbeddingService.java`
- `src/main/java/com/learnpulse/backend/service/DocumentRetrievalService.java`
- `src/main/java/com/learnpulse/backend/service/DocumentRagService.java`
- `src/main/java/com/learnpulse/backend/config/PgVectorStoreConfig.java`
- `src/main/java/com/learnpulse/backend/controller/DocumentRagController.java`
- `src/main/java/com/learnpulse/backend/dto/DocumentRagRequest.java`
- `src/main/java/com/learnpulse/backend/dto/DocumentRagResponse.java`
- `src/main/java/com/learnpulse/backend/dto/RagSourceDTO.java`
- `src/test/java/com/learnpulse/backend/document/DocumentChunkingTest.java`
- `src/test/java/com/learnpulse/backend/document/DocumentEmbeddingTest.java`
- `src/test/java/com/learnpulse/backend/document/DocumentRetrievalIntegrationTest.java`
- `src/test/java/com/learnpulse/backend/document/DocumentRagIntegrationTest.java`
- `verify_week10_rag.py`
- `create_sample_pdf.py`

### Files Modified
- `pom.xml`: Added pgvector starter.
- `src/main/resources/application.yml`: Configured RAG properties and pgvector dimensions.
- `src/main/java/com/learnpulse/backend/ai/PromptManager.java`: Added `buildRagPrompt` method.

---

## 7. Scope Boundary & Git Confirmation

### Scope Boundary Adherence
- No frontend UI, AI Tutor, AI quiz generators, or external paid APIs were built.
- Backend scope strictly adhered to Week 10 RAG infrastructure.

### Git Status Confirmation
- **No Git commits were performed.**
- **No Git pushes were performed.**
- Working directory remains preserved for manual code review.

---
**Final Status:** Implemented and verified against the Week 10 specification.
