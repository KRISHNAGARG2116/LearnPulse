package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.QuizRepository;
import com.learnpulse.backend.repository.StudentQuizResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizGradingService {

    private final QuizRepository quizRepository;
    private final StudentQuizResultRepository resultRepository;

    @Transactional
    public StudentQuizResultDTO submitQuiz(User student, QuizSubmissionRequest submission) {
        if (student == null) {
            throw new ApiException("Authentication required to submit quiz", HttpStatus.UNAUTHORIZED);
        }

        Quiz quiz = quizRepository.findById(submission.getQuizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with ID: " + submission.getQuizId()));

        Map<UUID, Question> questionMap = quiz.getQuestions().stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        int score = 0;
        int correctAnswers = 0;
        int wrongAnswers = 0;
        Set<UUID> processedQuestions = new HashSet<>();

        for (QuestionAnswerRequest answerReq : submission.getAnswers()) {
            UUID questionId = answerReq.getQuestionId();
            if (!questionMap.containsKey(questionId)) {
                throw new ApiException("Question ID " + questionId + " does not belong to Quiz ID " + quiz.getId(), HttpStatus.BAD_REQUEST);
            }

            if (processedQuestions.contains(questionId)) {
                continue; // Avoid duplicate scoring for the same question within a single submission
            }
            processedQuestions.add(questionId);

            Question question = questionMap.get(questionId);
            String selected = answerReq.getSelectedAnswer().trim().toUpperCase();

            if (question.getCorrectAnswer().equalsIgnoreCase(selected)) {
                score += question.getMarks();
                correctAnswers++;
            } else {
                wrongAnswers++;
            }
        }

        // Questions in quiz that were not attempted count as wrong answers
        int unattempted = quiz.getQuestions().size() - processedQuestions.size();
        if (unattempted > 0) {
            wrongAnswers += unattempted;
        }

        int totalMarks = quiz.getTotalMarks();
        double rawPercentage = totalMarks > 0 ? ((double) score / totalMarks) * 100.0 : 0.0;
        double percentage = BigDecimal.valueOf(rawPercentage)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        StudentQuizResult result = StudentQuizResult.builder()
                .student(student)
                .quiz(quiz)
                .score(score)
                .totalMarks(totalMarks)
                .percentage(percentage)
                .correctAnswers(correctAnswers)
                .wrongAnswers(wrongAnswers)
                .build();

        StudentQuizResult savedResult = resultRepository.save(result);
        log.info("Quiz submitted successfully by student ID: {} for Quiz ID: {}. Score: {}/{} ({}%)",
                student.getId(), quiz.getId(), score, totalMarks, percentage);

        return mapToDTO(savedResult);
    }

    @Transactional(readOnly = true)
    public StudentQuizResultDTO getResultById(User requester, UUID resultId) {
        StudentQuizResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz result not found with ID: " + resultId));

        // Ownership & Security Check: Student can only access their own result
        if (requester.getRole() == Role.STUDENT && !result.getStudent().getId().equals(requester.getId())) {
            throw new AccessDeniedException("Access Denied: You cannot view another student's quiz results");
        }

        return mapToDTO(result);
    }

    @Transactional(readOnly = true)
    public StudentProgressDTO getStudentProgress(User student) {
        if (student == null) {
            throw new ApiException("Authentication required to view student progress", HttpStatus.UNAUTHORIZED);
        }

        List<StudentQuizResult> results = resultRepository.findByStudentIdOrderByAttemptedAtDesc(student.getId());

        if (results.isEmpty()) {
            return StudentProgressDTO.builder()
                    .studentId(student.getId())
                    .studentEmail(student.getEmail())
                    .totalQuizzesAttempted(0)
                    .totalQuizzesCompleted(0)
                    .averageScore(0.0)
                    .averagePercentage(0.0)
                    .highestScore(0)
                    .totalCorrectAnswers(0)
                    .totalWrongAnswers(0)
                    .recentAttempts(Collections.emptyList())
                    .build();
        }

        int totalAttempts = results.size();
        Set<UUID> completedQuizzes = results.stream().map(r -> r.getQuiz().getId()).collect(Collectors.toSet());

        double totalScoreSum = results.stream().mapToInt(StudentQuizResult::getScore).sum();
        double totalPercentageSum = results.stream().mapToDouble(StudentQuizResult::getPercentage).sum();
        int highestScore = results.stream().mapToInt(StudentQuizResult::getScore).max().orElse(0);

        int totalCorrect = results.stream().mapToInt(StudentQuizResult::getCorrectAnswers).sum();
        int totalWrong = results.stream().mapToInt(StudentQuizResult::getWrongAnswers).sum();

        double avgScore = BigDecimal.valueOf(totalScoreSum / totalAttempts)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        double avgPercentage = BigDecimal.valueOf(totalPercentageSum / totalAttempts)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        List<StudentQuizResultDTO> recentDTOs = results.stream()
                .limit(10)
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return StudentProgressDTO.builder()
                .studentId(student.getId())
                .studentEmail(student.getEmail())
                .totalQuizzesAttempted(totalAttempts)
                .totalQuizzesCompleted(completedQuizzes.size())
                .averageScore(avgScore)
                .averagePercentage(avgPercentage)
                .highestScore(highestScore)
                .totalCorrectAnswers(totalCorrect)
                .totalWrongAnswers(totalWrong)
                .recentAttempts(recentDTOs)
                .build();
    }

    public StudentQuizResultDTO mapToDTO(StudentQuizResult result) {
        return StudentQuizResultDTO.builder()
                .id(result.getId())
                .quizId(result.getQuiz().getId())
                .quizTitle(result.getQuiz().getTitle())
                .studentId(result.getStudent().getId())
                .studentEmail(result.getStudent().getEmail())
                .score(result.getScore())
                .totalMarks(result.getTotalMarks())
                .percentage(result.getPercentage())
                .correctAnswers(result.getCorrectAnswers())
                .wrongAnswers(result.getWrongAnswers())
                .attemptedAt(result.getAttemptedAt())
                .build();
    }
}
