package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.ChapterProgressionDTO;
import com.learnpulse.backend.dto.CourseProgressionDTO;
import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseProgressionService {

    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final QuizRepository quizRepository;
    private final StudentQuizResultRepository resultRepository;

    @Transactional(readOnly = true)
    public CourseProgressionDTO getCourseProgression(User student, UUID courseId) {
        if (student == null) {
            throw new ApiException("Authentication required to view course progression", HttpStatus.UNAUTHORIZED);
        }

        Subject course = subjectRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));

        List<Chapter> chapters = chapterRepository.findBySubjectIdOrderByChapterNumberAsc(course.getId());

        List<ChapterProgressionDTO> chapterProgressions = new ArrayList<>();
        boolean previousChapterPassed = true; // Chapter 1 unlocked by default
        int completedChaptersCount = 0;
        Integer currentUnlockedChapterNumber = chapters.isEmpty() ? 0 : 1;

        for (Chapter ch : chapters) {
            boolean isUnlocked = previousChapterPassed;

            Optional<Quiz> quizOpt = quizRepository.findFirstByChapterId(ch.getId());
            UUID quizId = quizOpt.map(Quiz::getId).orElse(null);
            String quizTitle = quizOpt.map(Quiz::getTitle).orElse(null);

            Double highestScore = 0.0;
            boolean isQuizPassed = false;

            if (quizId != null) {
                List<StudentQuizResult> results = resultRepository.findByStudentIdAndQuizIdOrderByAttemptedAtDesc(student.getId(), quizId);
                if (!results.isEmpty()) {
                    highestScore = results.stream().mapToDouble(StudentQuizResult::getPercentage).max().orElse(0.0);
                    double passingScore = quizOpt.get().getPassingScorePercentage() != null ? quizOpt.get().getPassingScorePercentage() : 80.0;
                    isQuizPassed = highestScore >= passingScore;
                }
            } else {
                isQuizPassed = isUnlocked;
            }

            if (isQuizPassed && isUnlocked) {
                completedChaptersCount++;
            }

            if (isUnlocked) {
                currentUnlockedChapterNumber = ch.getChapterNumber();
            }

            chapterProgressions.add(ChapterProgressionDTO.builder()
                    .chapterId(ch.getId())
                    .title(ch.getTitle())
                    .chapterNumber(ch.getChapterNumber())
                    .description(ch.getDescription())
                    .isUnlocked(isUnlocked)
                    .isCompleted(isQuizPassed)
                    .quizId(quizId)
                    .quizTitle(quizTitle)
                    .highestScorePercentage(highestScore)
                    .isQuizPassed(isQuizPassed)
                    .build());

            previousChapterPassed = isUnlocked && isQuizPassed;
        }

        Optional<Quiz> finalQuizOpt = quizRepository.findFirstBySubjectIdAndChapterIdIsNull(course.getId());
        UUID finalQuizId = finalQuizOpt.map(Quiz::getId).orElse(null);
        String finalQuizTitle = finalQuizOpt.map(Quiz::getTitle).orElse(null);

        boolean allChaptersCompleted = !chapters.isEmpty() && completedChaptersCount == chapters.size();
        boolean isFinalQuizUnlocked = allChaptersCompleted;
        boolean isFinalQuizPassed = false;

        if (finalQuizId != null && isFinalQuizUnlocked) {
            List<StudentQuizResult> finalResults = resultRepository.findByStudentIdAndQuizIdOrderByAttemptedAtDesc(student.getId(), finalQuizId);
            if (!finalResults.isEmpty()) {
                double highestFinalScore = finalResults.stream().mapToDouble(StudentQuizResult::getPercentage).max().orElse(0.0);
                double passingScore = finalQuizOpt.get().getPassingScorePercentage() != null ? finalQuizOpt.get().getPassingScorePercentage() : 75.0;
                isFinalQuizPassed = highestFinalScore >= passingScore;
            }
        }

        boolean isCourseCompleted = allChaptersCompleted && (finalQuizId == null || isFinalQuizPassed);

        return CourseProgressionDTO.builder()
                .courseId(course.getId())
                .courseName(course.getName())
                .courseCode(course.getCode())
                .totalChapters(chapters.size())
                .completedChapters(completedChaptersCount)
                .currentUnlockedChapterNumber(currentUnlockedChapterNumber)
                .finalQuizId(finalQuizId)
                .finalQuizTitle(finalQuizTitle)
                .isFinalQuizUnlocked(isFinalQuizUnlocked)
                .isFinalQuizPassed(isFinalQuizPassed)
                .isCourseCompleted(isCourseCompleted)
                .chapters(chapterProgressions)
                .build();
    }

    @Transactional(readOnly = true)
    public void validateQuizAccess(User student, Quiz quiz) {
        if (student == null) {
            throw new ApiException("Authentication required to access quiz", HttpStatus.UNAUTHORIZED);
        }

        // 1. Publication check
        if (Boolean.FALSE.equals(quiz.getIsPublished()) || quiz.getStatus() == QuizStatus.DRAFT) {
            if (student.getRole() == Role.STUDENT) {
                throw new ApiException("Quiz is in draft mode and not published", HttpStatus.FORBIDDEN);
            }
        }

        // 2. Progression lock check for students
        if (student.getRole() == Role.STUDENT) {
            if (quiz.getQuizType() == QuizType.CHAPTER_QUIZ && quiz.getChapter() != null) {
                Chapter currentChapter = quiz.getChapter();
                Subject subject = currentChapter.getSubject();
                List<Chapter> chapters = chapterRepository.findBySubjectIdOrderByChapterNumberAsc(subject.getId());

                for (Chapter ch : chapters) {
                    if (ch.getChapterNumber() >= currentChapter.getChapterNumber()) {
                        break;
                    }
                    Optional<Quiz> chQuizOpt = quizRepository.findFirstByChapterId(ch.getId());
                    if (chQuizOpt.isPresent()) {
                        Quiz prevQuiz = chQuizOpt.get();
                        List<StudentQuizResult> results = resultRepository.findByStudentIdAndQuizIdOrderByAttemptedAtDesc(student.getId(), prevQuiz.getId());
                        double maxScore = results.stream().mapToDouble(StudentQuizResult::getPercentage).max().orElse(0.0);
                        double passing = prevQuiz.getPassingScorePercentage() != null ? prevQuiz.getPassingScorePercentage() : 80.0;
                        if (maxScore < passing) {
                            throw new ApiException("Prerequisite Chapter " + ch.getChapterNumber() + " quiz not passed. Chapter " + currentChapter.getChapterNumber() + " is locked.", HttpStatus.FORBIDDEN);
                        }
                    }
                }
            } else if (quiz.getQuizType() == QuizType.FINAL_COURSE_QUIZ && quiz.getSubject() != null) {
                Subject subject = quiz.getSubject();
                CourseProgressionDTO progression = getCourseProgression(student, subject.getId());
                if (!Boolean.TRUE.equals(progression.getIsFinalQuizUnlocked())) {
                    throw new ApiException("Final Course Quiz is locked until all chapter quizzes are successfully passed.", HttpStatus.FORBIDDEN);
                }
            }
        }
    }
}
