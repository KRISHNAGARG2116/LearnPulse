# WEEK 6 IMPLEMENTATION: ASSESSMENT & QUIZ ENGINE REPORT

## 1. OBJECTIVE & EXECUTIVE SUMMARY

Week 6 introduces a high-performance, secure, and transactional **Assessment and Quiz Engine** to the LearnPulse AI Learning Management System, complete with a **Single-Page Scrollable Quiz Experience** (Google Forms-style assessment).

The engine empowers educators (Teachers and Administrators) to author structured quizzes containing multiple-choice questions, and enables Students to attempt quizzes with real-time per-question answer selection, controlled single-question answer reveals, automatic server-side grading, persistent result tracking, and aggregated performance analytics.

### Key Objectives Accomplished:
- **Relational Assessment Data Model**: Persisted `Quiz`, `Question`, and `StudentQuizResult` JPA entities with UUID identifiers, transactional cascading, and foreign-key integrity.
- **Transactional Quiz Authoring**: Exposed `POST /api/teacher/create-quiz` for creating quizzes and nested questions atomically.
- **Secure Student Quiz Delivery**: Exposed `GET /api/quizzes` and `GET /api/quizzes/{id}` with subject/chapter filtering. Enforced strict data security preventing the exposure of `correctAnswer` in student DTOs.
- **Single-Page Scrollable Quiz Experience UI**: Developed a web client interface (`/quiz.html`) rendering all quiz questions vertically on a single scrollable page without forced "Next" wizard step navigation.
- **Invisible Correctness On Selection**: Selecting an option highlights the student's choice without revealing whether the choice is correct or wrong. No correctness feedback badges (green/red) are displayed prior to explicit reveal or final grading.
- **Controlled Answer Reveal Security**: Implemented `POST /api/quizzes/{quizId}/questions/{questionId}/reveal`. Enforces a strict server-side prerequisite ensuring a student MUST select an option before revealing the actual correct answer for that question (`Correct Answer: Option X`).
- **Scoring Independence**: Guaranteed that revealing a correct answer NEVER alters the student's selected answer or score. Final grading (`POST /api/quizzes/submit`) uses the student's actual selected options.
- **Result Ownership & Progress Analytics**: Implemented `GET /api/quizzes/result/{id}` (with strict student ownership enforcement) and `GET /api/student/progress` (providing aggregate student performance analytics).
- **Comprehensive Integration Test Suite**: Developed 39 automated integration tests verifying all 39 requirement test cases, bringing total project test count to 70 passing tests (`BUILD SUCCESS`).

---

## 2. ASSESSMENT SYSTEM ARCHITECTURE

```
+-----------------------------------------------------------------------------------+
|                        SINGLE SCROLLABLE QUIZ PAGE (/quiz.html)                   |
|  - Renders all questions vertically (Google Forms style)                           |
|  - Option selection records choice (invisible correctness) & enables [Reveal]     |
|  - [Reveal Answer] fetches actual correct answer choice (Option X)                |
|  - [Submit Quiz] sends selected options to backend grading                        |
+----------------------------------------+------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                                 CONTROLLER LAYER                                  |
|   TeacherQuizController (/api/teacher)      StudentQuizController (/api/quizzes)  |
+----------------------------------------+------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                                  SERVICE LAYER                                    |
|   QuizService (Authoring, Delivery, Reveal)  QuizGradingService (Server Grading)  |
+----------------------------------------+------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                                 REPOSITORY LAYER                                  |
|   QuizRepository           QuestionRepository       StudentQuizResultRepository   |
+----------------------------------------+------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                                 DATABASE LAYER                                    |
|   PostgreSQL Tables: quizzes, questions, student_quiz_results                     |
+-----------------------------------------------------------------------------------+
```

---

## 3. ASSESSMENT DATA MODEL & POSTGRESQL SCHEMA

### 3.1 Entity Relationships
```
        ┌──────────────┐ 1      * ┌──────────────┐
        │   Subject    ├──────────┤     Quiz     │
        └──────────────┘          └──────┬───────┘
        ┌──────────────┐ 1      *        │
        │   Chapter    ├─────────────────┤
        └──────────────┘                 │ 1
                                         │
                                         ├─────────────────────────┐ *
                                         │ *                       v
                                  ┌──────┴───────┐        ┌──────────────────┐
                                  │   Question   │        │StudentQuizResult │
                                  └──────────────┘        └─────────┬────────┘
                                                                    │ *
                                                          ┌─────────┴────────┐
                                                          │   User (Student) │
                                                          └──────────────────┘
```

### 3.2 Database Tables

#### Table: `quizzes`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | Unique quiz identifier |
| `title` | `VARCHAR(255)` | `NOT NULL` | Quiz title |
| `description` | `TEXT` | `NULLABLE` | Quiz overview / instructions |
| `subject_id` | `UUID` | `FOREIGN KEY (subjects.id)` | Optional subject classification |
| `chapter_id` | `UUID` | `FOREIGN KEY (chapters.id)` | Optional chapter classification |
| `total_marks` | `INTEGER` | `NOT NULL` | Aggregated total quiz marks |
| `created_by` | `UUID` | `FOREIGN KEY (users.id), NOT NULL` | Creator (Teacher/Admin) |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Creation timestamp |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Update timestamp |

#### Table: `questions`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | Unique question identifier |
| `quiz_id` | `UUID` | `FOREIGN KEY (quizzes.id), NOT NULL` | Parent quiz |
| `question_text`| `TEXT` | `NOT NULL` | Question stem |
| `option_a` | `VARCHAR(255)` | `NOT NULL` | Option A choice text |
| `option_b` | `VARCHAR(255)` | `NOT NULL` | Option B choice text |
| `option_c` | `VARCHAR(255)` | `NOT NULL` | Option C choice text |
| `option_d` | `VARCHAR(255)` | `NOT NULL` | Option D choice text |
| `correct_answer`| `VARCHAR(1)` | `NOT NULL` | Stored correct option ("A", "B", "C", "D") |
| `marks` | `INTEGER` | `NOT NULL, DEFAULT 1` | Question point weight |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Creation timestamp |
| `updated_at` | `TIMESTAMP` | `NOT NULL` | Update timestamp |

#### Table: `student_quiz_results`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY` | Unique result attempt identifier |
| `student_id` | `UUID` | `FOREIGN KEY (users.id), NOT NULL` | Authenticated student |
| `quiz_id` | `UUID` | `FOREIGN KEY (quizzes.id), NOT NULL` | Attempted quiz |
| `score` | `INTEGER` | `NOT NULL` | Earned marks |
| `total_marks` | `INTEGER` | `NOT NULL` | Total quiz marks |
| `percentage` | `DOUBLE PRECISION`| `NOT NULL` | Earned percentage (0.0 to 100.0) |
| `correct_answers`| `INTEGER` | `NOT NULL` | Count of correct choices |
| `wrong_answers` | `INTEGER` | `NOT NULL` | Count of incorrect choices |
| `attempted_at` | `TIMESTAMP` | `NOT NULL` | Attempt submission timestamp |

---

## 4. API SPECIFICATION & DATA DTOs

### 4.1 Quiz Authoring API
- **Endpoint**: `POST /api/teacher/create-quiz`
- **Security**: Requires `ROLE_TEACHER` or `ROLE_ADMIN` JWT token.

### 4.2 Quiz Delivery APIs (Student-Facing)
- **Endpoints**:
  - `GET /api/quizzes` (Supports `?subjectId=...&chapterId=...`)
  - `GET /api/quizzes/{id}`
- **Security**: Requires authenticated user JWT.
> **CRITICAL SECURITY GUARANTEE**: `correctAnswer` is strictly excluded from `StudentQuestionDTO` and initial quiz payloads.

### 4.3 Controlled Single-Question Answer Reveal API
- **Endpoint**: `POST /api/quizzes/{quizId}/questions/{questionId}/reveal`
- **Security**: Accepts authenticated student JWT.
- **Prerequisite Validation**: Validates that `selectedOption` ("A", "B", "C", "D") is provided. Returns `HTTP 400 Bad Request` if no option has been selected.
- **Request Body**:
```json
{
  "selectedOption": "B"
}
```
- **Response (`200 OK`)**:
```json
{
  "status": "success",
  "message": "Answer revealed successfully",
  "data": {
    "questionId": "86e236d5-5772-4c82-b419-84d074a865e9",
    "selectedOption": "B",
    "isCorrect": false,
    "correctAnswer": "C",
    "explanation": "The correct answer is Option C."
  }
}
```

### 4.4 Automatic Server-Side Grading API
- **Endpoint**: `POST /api/quizzes/submit`
- **Security**: Accepts authenticated user JWT. Identity is bound to `@AuthenticationPrincipal User student`.
- **Request Body**:
```json
{
  "quizId": "e568120c-6431-409a-ba6d-13d0f21ccb5d",
  "answers": [
    {
      "questionId": "86e236d5-5772-4c82-b419-84d074a865e9",
      "selectedAnswer": "B"
    }
  ]
}
```

### 4.5 Result & Progress Analytics APIs
- **GET `/api/quizzes/result/{id}`**: Retrieves specific attempt result. Enforces student ownership (`HTTP 403 Forbidden` if a student attempts to view another student's result).
- **GET `/api/student/progress`**: Returns aggregated analytics (`totalQuizzesAttempted`, `totalQuizzesCompleted`, `averageScore`, `averagePercentage`, `highestScore`, `totalCorrectAnswers`, `totalWrongAnswers`, and `recentAttempts`).

---

## 5. SINGLE-PAGE QUIZ INTERACTION ARCHITECTURE & UI

The web client (`/quiz.html`) provides a Google Forms-style assessment:
1. **Single Scrollable Page**: Displays all quiz questions vertically on the same page.
2. **Per-Question Independent State**: Selection records choice with **invisible correctness** (no green/red correctness badges).
3. **Prerequisite Protection**: `[ Reveal Answer ]` cannot be clicked before selecting an option.
4. **Actual Correct Answer Reveal**: Displays `Correct Answer: Option X` directly below the button.
5. **Scoring Independence**: Answer reveal displays explanation without modifying `selectedAnswer` or score.
6. **Top Result Summary Banner**: Submission displays Right Answers, Wrong Answers, and Percentage at the top of the view.

---

## 6. AUTOMATED INTEGRATION TEST RESULTS

Executing the complete Maven test suite:

```bash
.tools/apache-maven-3.9.6/bin/mvn clean test
```

### Maven Execution Output:
```
[INFO] Running com.learnpulse.backend.LearningAssistantApplicationTests (1 test)
[INFO] Running com.learnpulse.backend.security.JwtProviderTest (3 tests)
[INFO] Running com.learnpulse.backend.security.SecurityRbacIntegrationTest (7 tests)
[INFO] Running com.learnpulse.backend.assessment.AssessmentEngineIntegrationTest (39 tests)
[INFO] Running com.learnpulse.backend.document.DocumentIngestionIntegrationTest (9 tests)
[INFO] Running com.learnpulse.backend.academic.AcademicHierarchyIntegrationTest (5 tests)
[INFO] Running com.learnpulse.backend.controller.BaseStatusControllerTest (1 test)
[INFO] Running com.learnpulse.backend.profile.StudentTeacherProfileIntegrationTest (5 tests)
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 70, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  35.885 s
[INFO] Finished at: 2026-09-02T18:03:25+05:30
```

---

## 7. SCOPE COMPLIANCE & EXCLUSIONS

The Week 6 implementation strictly follows all scope boundaries set forth by the mentor specification:

- [x] **Backend Assessment Engine**: Fully implemented and verified.
- [x] **Single Scrollable Quiz UI Experience**: Fully implemented in `/quiz.html`.
- [x] **Invisible Option Correctness Selection**: No correctness feedback shown prior to reveal or submission.
- [x] **Controlled Reveal Answer Security**: Server-validated prerequisite option-selection enforcement.
- [x] **Zero Out-of-Scope Features**: Excluded RAG, embeddings, vector search, LLMs, AI Tutor, recommendations, notifications, and gamification.
- [x] **Preserved Existing Infrastructure**: Preserved Week 2-5 security, profile, academic hierarchy, and document upload features.
