package com.learnpulse.backend.service;

import com.learnpulse.backend.ai.PromptManager;
import com.learnpulse.backend.ai.StructuredAiOutputParser;
import com.learnpulse.backend.config.AiLimitsConstants;
import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.QuestionRepository;
import com.learnpulse.backend.repository.QuizRepository;
import com.learnpulse.backend.repository.TeacherSubjectRepository;
import com.learnpulse.backend.repository.UploadedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiQuizGenerationService {

    private final ChatClient chatClient;
    private final PromptManager promptManager;
    private final StructuredAiOutputParser structuredParser;
    private final UploadedDocumentRepository documentRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AiTelemetryService aiTelemetryService;

    @org.springframework.beans.factory.annotation.Autowired
    public AiQuizGenerationService(ChatClient chatClient,
                                   PromptManager promptManager,
                                   StructuredAiOutputParser structuredParser,
                                   UploadedDocumentRepository documentRepository,
                                   TeacherSubjectRepository teacherSubjectRepository,
                                   QuizRepository quizRepository,
                                   QuestionRepository questionRepository,
                                   @org.springframework.beans.factory.annotation.Autowired(required = false) AiTelemetryService aiTelemetryService) {
        this.chatClient = chatClient;
        this.promptManager = promptManager;
        this.structuredParser = structuredParser;
        this.documentRepository = documentRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.aiTelemetryService = aiTelemetryService;
    }

    public AiQuizGenerationService(ChatClient chatClient,
                                   PromptManager promptManager,
                                   StructuredAiOutputParser structuredParser,
                                   UploadedDocumentRepository documentRepository,
                                   TeacherSubjectRepository teacherSubjectRepository,
                                   QuizRepository quizRepository,
                                   QuestionRepository questionRepository) {
        this(chatClient, promptManager, structuredParser, documentRepository, teacherSubjectRepository, quizRepository, questionRepository, null);
    }

    @Transactional
    public GeneratedQuizResponse generateAndPersistQuiz(User currentUser, GenerateQuizRequest request) {
        // 1. Authorization & Role Verification
        if (currentUser == null) {
            throw new ApiException("Authentication required to generate quiz", HttpStatus.UNAUTHORIZED);
        }

        if (currentUser.getRole() == Role.STUDENT) {
            throw new ApiException("Students are not permitted to generate quizzes", HttpStatus.FORBIDDEN);
        }

        if (request == null || request.getDocumentId() == null) {
            throw new ApiException("Document ID is required", HttpStatus.BAD_REQUEST);
        }

        // 2. Validate Question Count Limits
        int targetQuestionCount = (request.getQuestionCount() != null) ? request.getQuestionCount() : 5;
        if (targetQuestionCount < 1 || targetQuestionCount > 20) {
            throw new ApiException("Question count must be between 1 and 20", HttpStatus.BAD_REQUEST);
        }

        // 3. Document Retrieval & Access Checks
        UUID docId = request.getDocumentId();
        UploadedDocument document = documentRepository.findByIdAndActiveTrue(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + docId));

        // Enforce Teacher-Subject Ownership / Assignment
        if (currentUser.getRole() == Role.TEACHER) {
            boolean isUploader = document.getTeacher() != null && document.getTeacher().getId().equals(currentUser.getId());
            boolean isAssignedToSubject = document.getSubject() != null &&
                    teacherSubjectRepository.existsByTeacherIdAndSubjectId(currentUser.getId(), document.getSubject().getId());

            if (!isUploader && !isAssignedToSubject) {
                log.warn("Teacher {} denied access to generate quiz from document ID {}", currentUser.getEmail(), docId);
                throw new ApiException("Access denied: You are not authorized to generate quizzes from this document", HttpStatus.FORBIDDEN);
            }
        }

        // 4. Validate Document Content Availability
        String extractedText = document.getExtractedText();
        if (!StringUtils.hasText(extractedText)) {
            throw new ApiException("Document text content is empty or has not been extracted yet", HttpStatus.BAD_REQUEST);
        }

        String contentForPrompt = extractedText.trim();
        if (contentForPrompt.length() > AiLimitsConstants.MAX_QUIZ_GEN_CHARS) {
            contentForPrompt = contentForPrompt.substring(0, AiLimitsConstants.MAX_QUIZ_GEN_CHARS);
        }

        log.info("Generating AI quiz for user {} from document '{}' (ID: {}) with target question count: {}",
                currentUser.getEmail(), document.getOriginalFileName(), docId, targetQuestionCount);

        // 5. Construct AI Prompt & Call Ollama / Spring AI
        Prompt prompt = promptManager.buildQuizGenerationPrompt(
                contentForPrompt,
                targetQuestionCount,
                request.getDifficulty(),
                request.getTopic()
        );

        String rawOutput;
        Instant startTime = Instant.now();
        try {
            org.springframework.ai.chat.ChatResponse chatResponse = chatClient.call(prompt);
            rawOutput = (chatResponse != null && chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null)
                    ? chatResponse.getResult().getOutput().getContent()
                    : null;
            int promptTokens;
            int compTokens;
            String provenance;
            if (chatResponse != null && chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null
                    && chatResponse.getMetadata().getUsage().getPromptTokens() != null && chatResponse.getMetadata().getUsage().getPromptTokens() > 0) {
                promptTokens = chatResponse.getMetadata().getUsage().getPromptTokens().intValue();
                compTokens = chatResponse.getMetadata().getUsage().getGenerationTokens() != null ? chatResponse.getMetadata().getUsage().getGenerationTokens().intValue() : 0;
                provenance = "PROVIDER_REPORTED";
            } else {
                promptTokens = Math.max(1, contentForPrompt.length() / 4);
                compTokens = (rawOutput != null) ? Math.max(1, rawOutput.length() / 4) : 0;
                provenance = "LOCAL_ESTIMATION";
            }
            if (aiTelemetryService != null) {
                aiTelemetryService.recordOperation("AI_QUIZ_GENERATION", "qwen2.5-coder:7b", currentUser, startTime, Instant.now(), "SUCCESS", promptTokens, compTokens, provenance, null);
            }
        } catch (Exception ex) {
            log.error("AI service failure during quiz generation: {}", ex.getMessage(), ex);
            if (aiTelemetryService != null) {
                aiTelemetryService.recordOperation("AI_QUIZ_GENERATION", "qwen2.5-coder:7b", currentUser, startTime, Instant.now(), "FAILED", Math.max(1, contentForPrompt.length() / 4), 0, "LOCAL_ESTIMATION", ex.getMessage());
            }
            throw new ApiException("The AI quiz generation service is currently offline or unreachable. Please try again later.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        // 6. Parse and Validate AI Response Structure
        GeneratedQuizAiResponse aiResponse = structuredParser.parseAndValidate(rawOutput, GeneratedQuizAiResponse.class);

        if (aiResponse == null || CollectionUtils.isEmpty(aiResponse.getQuestions())) {
            throw new ApiException("The AI service generated an empty quiz question set", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 7. Deduplicate and Validate Question Invariants
        List<GeneratedQuizQuestionDto> validatedQuestions = validateAndFilterQuestions(aiResponse.getQuestions());
        if (validatedQuestions.isEmpty()) {
            throw new ApiException("The AI service output could not produce any valid multiple-choice questions", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 8. Determine Quiz Title & Metadata
        String quizTitle = StringUtils.hasText(request.getQuizTitle())
                ? request.getQuizTitle().trim()
                : (StringUtils.hasText(aiResponse.getTitle()) ? aiResponse.getTitle().trim() : "AI Quiz: " + document.getOriginalFileName());

        Double passingScore = request.getPassingScorePercentage() != null ? request.getPassingScorePercentage() : 80.0;
        QuizType quizType = (document.getChapter() != null) ? QuizType.CHAPTER_QUIZ : QuizType.FINAL_COURSE_QUIZ;

        // 9. Persist Quiz and Questions in Database
        Quiz quiz = Quiz.builder()
                .title(quizTitle)
                .description("AI-generated assessment grounded in document: " + document.getOriginalFileName())
                .subject(document.getSubject())
                .chapter(document.getChapter())
                .quizType(quizType)
                .status(QuizStatus.DRAFT)      // DRAFT by default so teacher can review before publication
                .isPublished(false)            // Hidden from students until published
                .passingScorePercentage(passingScore)
                .createdBy(currentUser)
                .totalMarks(0)
                .build();

        Quiz savedQuiz = quizRepository.save(quiz);

        int totalMarks = 0;
        List<Question> questionEntities = new ArrayList<>();
        for (GeneratedQuizQuestionDto qDto : validatedQuestions) {
            int marks = (qDto.getMarks() != null && qDto.getMarks() > 0) ? qDto.getMarks() : 1;
            totalMarks += marks;

            Question question = Question.builder()
                    .quiz(savedQuiz)
                    .questionText(qDto.getQuestionText().trim())
                    .optionA(qDto.getOptionA().trim())
                    .optionB(qDto.getOptionB().trim())
                    .optionC(qDto.getOptionC().trim())
                    .optionD(qDto.getOptionD().trim())
                    .correctAnswer(qDto.getCorrectAnswer().trim().toUpperCase())
                    .explanation(qDto.getExplanation() != null ? qDto.getExplanation().trim() : null)
                    .source(QuestionSource.AI_GENERATED)
                    .marks(marks)
                    .build();

            questionEntities.add(question);
        }

        List<Question> savedQuestions = questionRepository.saveAll(questionEntities);
        savedQuiz.setTotalMarks(totalMarks);
        savedQuiz.setQuestions(savedQuestions);
        quizRepository.save(savedQuiz);

        log.info("Successfully generated and persisted AI Quiz ID {} with {} questions for teacher {}",
                savedQuiz.getId(), savedQuestions.size(), currentUser.getEmail());

        // 10. Map to Teacher Response DTO
        List<TeacherQuestionDTO> questionDTOs = savedQuestions.stream()
                .map(q -> TeacherQuestionDTO.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .optionA(q.getOptionA())
                        .optionB(q.getOptionB())
                        .optionC(q.getOptionC())
                        .optionD(q.getOptionD())
                        .correctAnswer(q.getCorrectAnswer())
                        .explanation(q.getExplanation())
                        .source(q.getSource() != null ? q.getSource().name() : null)
                        .marks(q.getMarks())
                        .build())
                .collect(Collectors.toList());

        return GeneratedQuizResponse.builder()
                .quizId(savedQuiz.getId())
                .title(savedQuiz.getTitle())
                .description(savedQuiz.getDescription())
                .subjectId(savedQuiz.getSubject() != null ? savedQuiz.getSubject().getId() : null)
                .subjectName(savedQuiz.getSubject() != null ? savedQuiz.getSubject().getName() : null)
                .chapterId(savedQuiz.getChapter() != null ? savedQuiz.getChapter().getId() : null)
                .chapterTitle(savedQuiz.getChapter() != null ? savedQuiz.getChapter().getTitle() : null)
                .documentId(docId)
                .documentFileName(document.getOriginalFileName())
                .status(savedQuiz.getStatus())
                .isPublished(savedQuiz.getIsPublished())
                .passingScorePercentage(savedQuiz.getPassingScorePercentage())
                .totalMarks(savedQuiz.getTotalMarks())
                .questionsCount(questionDTOs.size())
                .questions(questionDTOs)
                .createdAt(savedQuiz.getCreatedAt())
                .build();
    }

    private List<GeneratedQuizQuestionDto> validateAndFilterQuestions(List<GeneratedQuizQuestionDto> questions) {
        if (CollectionUtils.isEmpty(questions)) {
            return Collections.emptyList();
        }

        Map<String, GeneratedQuizQuestionDto> uniqueQuestions = new LinkedHashMap<>();

        for (GeneratedQuizQuestionDto q : questions) {
            if (q == null) continue;

            String qText = q.getQuestionText() != null ? q.getQuestionText().trim() : "";
            if (!StringUtils.hasText(qText)) continue;

            String optA = q.getOptionA() != null ? q.getOptionA().trim() : "";
            String optB = q.getOptionB() != null ? q.getOptionB().trim() : "";
            String optC = q.getOptionC() != null ? q.getOptionC().trim() : "";
            String optD = q.getOptionD() != null ? q.getOptionD().trim() : "";

            if (!StringUtils.hasText(optA) || !StringUtils.hasText(optB) ||
                !StringUtils.hasText(optC) || !StringUtils.hasText(optD)) {
                continue;
            }

            // Verify all 4 options are distinct
            Set<String> distinctOptions = new HashSet<>(Arrays.asList(
                    optA.toLowerCase(), optB.toLowerCase(), optC.toLowerCase(), optD.toLowerCase()
            ));
            if (distinctOptions.size() < 4) {
                log.warn("Skipping question with duplicate options: '{}'", qText);
                continue;
            }

            // Verify correct answer is one of A, B, C, D
            String answerKey = q.getCorrectAnswer() != null ? q.getCorrectAnswer().trim().toUpperCase() : "";
            if (!Arrays.asList("A", "B", "C", "D").contains(answerKey)) {
                log.warn("Skipping question with invalid correct answer key '{}'", answerKey);
                continue;
            }

            // Deduplicate questions by normalized question text
            String normalizedKey = qText.toLowerCase();
            uniqueQuestions.putIfAbsent(normalizedKey, q);
        }

        return new ArrayList<>(uniqueQuestions.values());
    }
}
