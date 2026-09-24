# LearnPulse LMS — Week 9 Implementation Report
**AI Infrastructure, Spring AI / Ollama & Qwen Model Provisioning**

---

## 1. Executive Summary & Objective

Week 9 establishes the foundational **AI Backend Infrastructure** for the LearnPulse AI-Powered Learning Platform. This release integrates Spring AI with a locally provisioned **Ollama** LLM instance executing the **Qwen** model (`qwen2.5-coder:7b`).

The primary objective of Week 9 is to deliver a secure, robust, multi-turn AI Tutor service with context management, chat history persistence, and failure isolation:
- **Architecture**: Enforces clean layering (`Controller -> AiLlmService -> PromptManager -> ChatClient -> Ollama -> Qwen`).
- **Resilience**: Complete failure isolation ensuring that Ollama or model unavailability never crashes the LMS platform or exposes raw internal errors to clients.
- **Persistence**: Persists successful AI chat interactions into PostgreSQL database table `ai_chat_history`.
- **Security**: Protects `POST /api/ai/chat` using existing JWT authentication and Role-Based Access Control (`ROLE_STUDENT`).

### Core Target Flow

```
Student Client
   │ (POST /api/ai/chat + JWT Bearer)
   ▼
AI Chat Controller (AiChatController.java)
   │ (Request Validation @Valid)
   ▼
AI Service Layer (AiLlmService.java)
   │ (Multi-Turn History Retrieval & Context Isolation)
   ▼
Prompt Management (PromptManager.java)
   │ (System Prompt + History Messages + User Input)
   ▼
Spring AI ChatClient (AiConfig.java / OllamaChatClient)
   │ (HTTP RestClient Communication)
   ▼
Local Ollama Service (v0.34.3)
   │
   ▼
Qwen Model (qwen2.5-coder:7b)
   │
   ▼
AI Tutor Response Generation
   │
   ▼
Chat History Persistence (ai_chat_history PostgreSQL table)
   │
   ▼
Standard API Response (ApiResponse<AiChatResponse>)
```

---

## 2. Local AI Environment Provisioning & Verification (Day 49)

The local Ollama environment and Qwen model were provisioned and empirically verified on the development environment:

- **Ollama Runtime**: Ollama version `0.34.3` installed and running as a local service at `http://localhost:11434`.
- **Provisioned Model**: `qwen2.5-coder:7b` (Parameter Size: 7.6B, Quantization: Q4_K_M, Context Window: 32,768 tokens, Digest: `dae161e27b0e90dd1856c8bb3209201fd6736d8eb66298e75ed87571486f4364`).
- **Direct HTTP API Verification**: Tested directly via `POST http://localhost:11434/api/generate`.

### Empirical Verification Record

| Metric / Parameter | Value / Result |
| :--- | :--- |
| **Ollama Service Endpoint** | `http://localhost:11434` |
| **Ollama Version** | `0.34.3` |
| **Active Model / Tag** | `qwen2.5-coder:7b` |
| **Test Prompt** | *"Explain Java inheritance in two sentences."* |
| **Model Output** | *"Java inheritance allows a class to inherit attributes and methods from another class, facilitating code reuse and the creation of a class hierarchy."* |
| **Ollama Cold Model Load** | approximately 44.4 seconds (initial memory load) |
| **Warm Direct Inference** | approximately 5.77 seconds (direct Ollama prompt generation) |
| **Observed Live Multi-Turn API Request** | approximately 58.80 seconds (live end-to-end HTTP request) |
| **Service Status** | **ACTIVE & FUNCTIONAL** |

*Note: Local inference latency is hardware/model dependent.*

---

## 3. Spring AI Integration & Architecture (Day 50 & 51)

### 3.1 Maven Dependencies (`pom.xml`)
Added Spring AI BOM version `0.8.1` and `spring-ai-ollama-spring-boot-starter` compatible with Spring Boot `3.2.5` without forcing unnecessary framework upgrades:

```xml
<properties>
    <spring-ai.version>0.8.1</spring-ai.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 3.2 Externalized Configuration (`application.yml`)
Configured environment properties with sensible development defaults, preventing hardcoded internal URLs or credentials:

```yaml
spring:
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        options:
          model: ${OLLAMA_MODEL:qwen2.5-coder:7b}
          temperature: ${OLLAMA_TEMPERATURE:0.7}
```

### 3.3 ChatClient Spring Configuration (`AiConfig.java`)
Created `AiConfig` to cleanly instantiate `OllamaApi` and `OllamaChatClient` beans exposed via the Spring AI `ChatClient` interface marked with `@Primary`.

### 3.4 Service Layer (`AiLlmService.java`)
Implemented `AiLlmService` to enforce strict separation of concerns:
- Controller never calls Ollama or ChatClient directly.
- Handles LLM invocation, prompt assembly, history retrieval, database persistence, and graceful fallback handling.
- **Failure Isolation**: Wraps LLM calls in safe `try-catch` blocks. If Ollama is unreachable, `AiLlmService` returns a controlled user-friendly response (`"The AI tutor service is currently offline or unreachable..."`) with `fallback: true` without throwing raw connection exceptions or taking down the LMS.

---

## 4. Prompt Management Architecture (Day 52)

Created `PromptManager` (`com.learnpulse.backend.ai.PromptManager`) to separate system prompt rules, conversation history, and user input:

- **System Instruction**: Establishes the Academic Tutor persona:
  - Supportive, encouraging, patient, and clear.
  - Explains concepts step-by-step with relatable examples.
  - Stays strictly within an educational context.
  - Asks for clarification if user input is ambiguous.
  - Avoids presenting unverified speculation as factual certainty.
- **Dynamic Subject Context**: Injects optional subject focus parameter (`subjectName`) into the system prompt when provided.
- **Message Layering**: Assembles `SystemMessage`, prior conversation history (`UserMessage` / `AssistantMessage`), and current `UserMessage` into a unified Spring AI `Prompt` object.

---

## 5. AI Tutor API & Security (Day 53)

### 5.1 Endpoint Specification
- **URL**: `POST /api/ai/chat`
- **Security**: Protected by Spring Security & JWT Filter; restricted to authenticated `ROLE_STUDENT` users.
- **Request DTO (`AiChatRequest`)**:
  - `message`: `@NotBlank`, `@Size(max = 2000)`
  - `conversationId`: Optional session UUID string for multi-turn tracking.
  - `subjectName`: Optional subject context.
- **Response DTO (`AiChatResponse`)**:
  - Encapsulated inside standard `ApiResponse<AiChatResponse>`.
  - Contains `conversationId`, `userMessage`, `aiResponse`, `timestamp`, and `fallback` boolean flag.

### 5.2 PostgreSQL Database Persistence
- **Entity**: `AiChatHistory` (`ai_chat_history` table).
- **Columns**: `id` (UUID), `user_id` (FK to `users`), `conversation_id`, `user_message` (TEXT), `ai_response` (TEXT), `created_at` (TIMESTAMP).
- **Student Conversation Isolation**: Indexed by `(user_id, conversation_id)` ensuring strict Student Conversation Isolation. LearnPulse is not a multi-tenant system; isolation is achieved via user_id + conversation_id filtering so students can only access their own conversation history.

### 5.3 Multi-Turn Conversation Context
- Injects up to the last 10 historical conversation turns for the given `(userId, conversationId)` into the `PromptManager`.
- Prevents prompt token explosion by bounding history length while maintaining context continuity across follow-up queries.

---

## 6. Comprehensive Automated Test Suite (Day 54)

Created `AiChatIntegrationTest` (`com.learnpulse.backend.ai.AiChatIntegrationTest`) validating test cases AI-001 through AI-010.

### Deterministic Test Design Rule
To ensure the backend regression test suite (`mvn test`) remains 100% deterministic and independent of whether Ollama is running during build execution, `AiChatIntegrationTest` utilizes `@MockBean` for the `ChatClient`. Genuine manual HTTP calls against the live local Qwen model were performed separately.

### Test Suite Execution Breakdown

| Test ID | Test Case Scenario | Setup / Input | Expected Result | Actual Result | Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **AI-001** | Basic AI Chat Request | Authenticated Student, valid query | HTTP 200 OK, valid AI response text, `fallback: false` | Passed | **PASS** |
| **AI-002** | Input Validation Rejection | Blank/whitespace message `"   "` | HTTP 400 Bad Request, standard validation error | Passed | **PASS** |
| **AI-003** | Ollama Service Unavailable | ChatClient throws connection exception | HTTP 200 OK, controlled fallback text, `fallback: true` | Passed | **PASS** |
| **AI-004** | Multi-Turn Conversation | Pre-existing conversation ID in DB | HTTP 200 OK, includes prior history in prompt | Passed | **PASS** |
| **AI-005** | Basic Sequential Stability Testing | Sequential chat requests (no concurrency) | HTTP 200 OK, stable response for each turn | Passed | **PASS** |
| **AI-006** | Chat History Persistence | Successful AI chat invocation | Saved record in `ai_chat_history` table | Passed | **PASS** |
| **AI-007** | Unauthenticated Rejection | Request without JWT Bearer header | HTTP 401 Unauthorized | Passed | **PASS** |
| **AI-008** | Non-Student RBAC Guard | Authenticated Teacher (`ROLE_TEACHER`) | HTTP 403 Forbidden | Passed | **PASS** |
| **AI-009** | Large Input Validation | Message text exceeding 2000 chars | HTTP 400 Bad Request, size constraint error | Passed | **PASS** |
| **AI-010** | Standard ApiResponse Format | Valid request | HTTP 200 OK, `status: success`, timestamp present | Passed | **PASS** |

### Complete Project Test Results (`mvn test`)
- **Total Tests Executed**: **94** (84 Week 1–8 tests + 10 Week 9 AI tests)
- **Failures**: **0**
- **Errors**: **0**
- **Skipped**: **0**
- **Build Outcome**: **BUILD SUCCESS** (Total time: 46.617 seconds)

---

## 7. Empirical Live Verification & Basic Sequential Stability Results

Live verification and basic sequential stability testing were performed against the running Spring Boot server on port `8080` communicating with local Ollama `0.34.3` executing `qwen2.5-coder:7b`:

1. **Student Registration & Auth**:
   - `POST /api/auth/register` -> HTTP 201 Created.
   - `POST /api/auth/login` -> HTTP 200 OK (JWT token acquired).
2. **Turn 1 AI Tutor Request**:
   - `POST /api/ai/chat` (`"What is polymorphism in Java?"`)
   - **Response**: HTTP 200 OK (`conversationId: "4932e028-59d4-496c-829c-ed19bead685a"`). Detailed OOP explanation returned with `Animal`, `Dog`, `Cat` Java code examples.
3. **Turn 2 Multi-Turn Follow-up Request**:
   - `POST /api/ai/chat` (`"Give me a concise code example for that."`, same `conversationId`)
   - **Response**: HTTP 200 OK. Model successfully referenced prior context from Turn 1 and provided a concise main method code block.
4. **Unauthenticated Rejection**:
   - `POST /api/ai/chat` without token -> HTTP 401 Unauthorized (`"Full authentication is required to access this resource"`).
5. **Validation Error Check**:
   - `POST /api/ai/chat` with whitespace message -> HTTP 400 Bad Request (`"Message cannot be empty or whitespace"`).
6. **Database Persistence Verification**:
   - Records `b9c202c3-3199-4c68-80a4-e1e389d2e5c5` and `3bcb9d9b-deda-47df-85b9-15f9dfe988a7` verified persisted in PostgreSQL `ai_chat_history` table.
7. **Basic Sequential Stability & Observed Performance Metrics**:
   - **Stability Scope**: Basic sequential stability testing was performed using multiple sequential chat requests to verify session consistency and context retention across turns without state corruption or memory leaks. No concurrent load testing was performed.
   - **Observed Local Inference Performance**:
     - **Ollama cold model load**: approximately 44.4 seconds
     - **Warm direct inference**: approximately 5.77 seconds
     - **Observed live multi-turn API request**: approximately 58.80 seconds
   - *Note: Local inference latency is hardware/model dependent.*

---

## 8. Summary of Files & Modifications

### Files Added (9 total)
1. `src/main/java/com/learnpulse/backend/entity/AiChatHistory.java` — Entity for persisting chat history.
2. `src/main/java/com/learnpulse/backend/repository/AiChatHistoryRepository.java` — JPA repository for chat history queries.
3. `src/main/java/com/learnpulse/backend/dto/AiChatRequest.java` — Validation DTO for AI chat requests.
4. `src/main/java/com/learnpulse/backend/dto/AiChatResponse.java` — Response DTO for AI chat output.
5. `src/main/java/com/learnpulse/backend/ai/PromptManager.java` — Component for system prompts & prompt building.
6. `src/main/java/com/learnpulse/backend/config/AiConfig.java` — Spring AI ChatClient bean configuration.
7. `src/main/java/com/learnpulse/backend/service/AiLlmService.java` — Service layer for LLM interaction & fallback handling.
8. `src/main/java/com/learnpulse/backend/controller/AiChatController.java` — REST controller for `POST /api/ai/chat`.
9. `src/test/java/com/learnpulse/backend/ai/AiChatIntegrationTest.java` — Automated integration test suite.

### Files Modified (3 total)
1. `pom.xml` — Added Spring AI BOM (`0.8.1`), `spring-ai-ollama-spring-boot-starter`, and Spring Milestones repository.
2. `src/main/resources/application.yml` — Added `spring.ai.ollama` configuration section.
3. `src/main/java/com/learnpulse/backend/security/SecurityConfig.java` — Added `/api/ai/**` security matcher requiring `ROLE_STUDENT`.

---

## 9. Explicit Scope Exclusions & Known Limitations

### Scope Exclusions Enforced
- **No RAG or Vector Search**: Document embeddings, pgvector retrieval, and PDF-grounded Q&A belong to Week 10+ and were not added.
- **No AI Quiz Generation**: Automated quiz/question generation from materials is out of scope for Week 9.
- **No Advanced AI Tutor Frontend UI**: Week 9 is strictly backend AI infrastructure.
- **No Git Commit / Push**: All changes remain in the local workspace.

---

## 10. Sign-Off & Status

| Verification Gate | Status | Empirical Results |
| :--- | :---: | :--- |
| **Backend Integration Tests** | **PASSED** | **94/94 tests passed** (`mvn test` BUILD SUCCESS) |
| **Local Ollama & Qwen Environment** | **VERIFIED** | Ollama v0.34.3 + `qwen2.5-coder:7b` active & responsive |
| **Multi-Turn Chat & DB Persistence** | **VERIFIED** | Context preserved across turns & saved in PostgreSQL |
| **Failure Isolation & Fallbacks** | **VERIFIED** | Controlled fallback response returned without crashing LMS |
| **Security & Validation** | **VERIFIED** | HTTP 401 on unauthenticated access; HTTP 400 on invalid input |
| **Mentor Requirements** | **VERIFIED** | Implemented and verified against the Week 9 specification |

**Mentor Requirements: Implemented and verified against the Week 9 specification**
