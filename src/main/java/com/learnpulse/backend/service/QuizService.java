package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.ChapterRepository;
import com.learnpulse.backend.repository.QuestionRepository;
import com.learnpulse.backend.repository.QuizRepository;
import com.learnpulse.backend.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;

    @Transactional
    public TeacherQuizDTO createQuiz(User creator, CreateQuizRequest request) {
        if (creator == null) {
            throw new ApiException("Authentication required to create quiz", HttpStatus.UNAUTHORIZED);
        }

        if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
            throw new ApiException("Quiz must contain at least one question", HttpStatus.BAD_REQUEST);
        }

        Subject subject = null;
        if (request.getSubjectId() != null) {
            subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));
        }

        Chapter chapter = null;
        if (request.getChapterId() != null) {
            chapter = chapterRepository.findById(request.getChapterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with ID: " + request.getChapterId()));
            if (subject != null && !chapter.getSubject().getId().equals(subject.getId())) {
                throw new ApiException("Chapter does not belong to the specified subject", HttpStatus.BAD_REQUEST);
            }
        }

        int calculatedTotalMarks = 0;
        List<Question> questionEntities = new ArrayList<>();

        Quiz quiz = Quiz.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .subject(subject)
                .chapter(chapter)
                .createdBy(creator)
                .totalMarks(0)
                .build();

        for (CreateQuestionRequest qReq : request.getQuestions()) {
            int qMarks = (qReq.getMarks() != null && qReq.getMarks() > 0) ? qReq.getMarks() : 1;
            calculatedTotalMarks += qMarks;

            Question question = Question.builder()
                    .quiz(quiz)
                    .questionText(qReq.getQuestionText().trim())
                    .optionA(qReq.getOptionA().trim())
                    .optionB(qReq.getOptionB().trim())
                    .optionC(qReq.getOptionC().trim())
                    .optionD(qReq.getOptionD().trim())
                    .correctAnswer(qReq.getCorrectAnswer().trim().toUpperCase())
                    .marks(qMarks)
                    .build();

            questionEntities.add(question);
        }

        quiz.setTotalMarks(calculatedTotalMarks);
        quiz.setQuestions(questionEntities);

        Quiz savedQuiz = quizRepository.save(quiz);
        log.info("Quiz created successfully with ID: {} and total marks: {}", savedQuiz.getId(), savedQuiz.getTotalMarks());

        return mapToTeacherDTO(savedQuiz);
    }

    @Transactional(readOnly = true)
    public RevealAnswerResponseDTO revealAnswerForQuestion(User student, UUID quizId, UUID questionId, RevealAnswerRequest request) {
        if (student == null) {
            throw new ApiException("Authentication required to reveal answer", HttpStatus.UNAUTHORIZED);
        }

        if (request == null || request.getSelectedOption() == null || request.getSelectedOption().trim().isEmpty()) {
            throw new ApiException("An option must be selected before revealing the correct answer", HttpStatus.BAD_REQUEST);
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with ID: " + quizId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with ID: " + questionId));

        if (!question.getQuiz().getId().equals(quiz.getId())) {
            throw new ApiException("Question ID " + questionId + " does not belong to Quiz ID " + quizId, HttpStatus.BAD_REQUEST);
        }

        String selected = request.getSelectedOption().trim().toUpperCase();
        if (!selected.matches("^[A-D]$")) {
            throw new ApiException("Invalid option selected. Must be 'A', 'B', 'C', or 'D'", HttpStatus.BAD_REQUEST);
        }

        boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(selected);

        return RevealAnswerResponseDTO.builder()
                .questionId(question.getId())
                .selectedOption(selected)
                .isCorrect(isCorrect)
                .correctAnswer(question.getCorrectAnswer())
                .explanation("The correct answer is Option " + question.getCorrectAnswer() + ".")
                .build();
    }

    @Transactional(readOnly = true)
    public List<StudentQuizDTO> getQuizzesForStudent(UUID subjectId, UUID chapterId) {
        List<Quiz> quizzes;
        if (subjectId != null && chapterId != null) {
            quizzes = quizRepository.findBySubjectIdAndChapterId(subjectId, chapterId);
        } else if (subjectId != null) {
            quizzes = quizRepository.findBySubjectId(subjectId);
        } else if (chapterId != null) {
            quizzes = quizRepository.findByChapterId(chapterId);
        } else {
            quizzes = quizRepository.findAll();
        }

        return quizzes.stream()
                .map(this::mapToStudentDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentQuizDTO getQuizByIdForStudent(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with ID: " + quizId));
        return mapToStudentDTO(quiz);
    }

    @Transactional(readOnly = true)
    public TeacherQuizDTO getQuizByIdForTeacher(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with ID: " + quizId));
        return mapToTeacherDTO(quiz);
    }

    public StudentQuizDTO mapToStudentDTO(Quiz quiz) {
        List<StudentQuestionDTO> studentQuestions = quiz.getQuestions().stream()
                .map(q -> StudentQuestionDTO.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .optionA(q.getOptionA())
                        .optionB(q.getOptionB())
                        .optionC(q.getOptionC())
                        .optionD(q.getOptionD())
                        .marks(q.getMarks())
                        .build())
                .collect(Collectors.toList());

        return StudentQuizDTO.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .subjectId(quiz.getSubject() != null ? quiz.getSubject().getId() : null)
                .subjectName(quiz.getSubject() != null ? quiz.getSubject().getName() : null)
                .chapterId(quiz.getChapter() != null ? quiz.getChapter().getId() : null)
                .chapterTitle(quiz.getChapter() != null ? quiz.getChapter().getTitle() : null)
                .totalMarks(quiz.getTotalMarks())
                .createdById(quiz.getCreatedBy().getId())
                .createdByEmail(quiz.getCreatedBy().getEmail())
                .questions(studentQuestions)
                .createdAt(quiz.getCreatedAt())
                .build();
    }

    public TeacherQuizDTO mapToTeacherDTO(Quiz quiz) {
        List<TeacherQuestionDTO> teacherQuestions = quiz.getQuestions().stream()
                .map(q -> TeacherQuestionDTO.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .optionA(q.getOptionA())
                        .optionB(q.getOptionB())
                        .optionC(q.getOptionC())
                        .optionD(q.getOptionD())
                        .correctAnswer(q.getCorrectAnswer())
                        .marks(q.getMarks())
                        .build())
                .collect(Collectors.toList());

        return TeacherQuizDTO.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .subjectId(quiz.getSubject() != null ? quiz.getSubject().getId() : null)
                .subjectName(quiz.getSubject() != null ? quiz.getSubject().getName() : null)
                .chapterId(quiz.getChapter() != null ? quiz.getChapter().getId() : null)
                .chapterTitle(quiz.getChapter() != null ? quiz.getChapter().getTitle() : null)
                .totalMarks(quiz.getTotalMarks())
                .createdById(quiz.getCreatedBy().getId())
                .createdByEmail(quiz.getCreatedBy().getEmail())
                .questions(teacherQuestions)
                .createdAt(quiz.getCreatedAt())
                .build();
    }
}
