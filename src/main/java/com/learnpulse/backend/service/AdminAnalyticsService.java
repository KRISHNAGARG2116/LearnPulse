package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.CourseActivityDTO;
import com.learnpulse.backend.dto.PlatformSummaryDTO;
import com.learnpulse.backend.dto.UserActivityTrendDTO;
import com.learnpulse.backend.entity.EnrollmentStatus;
import com.learnpulse.backend.entity.QuizStatus;
import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.Subject;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final QuizRepository quizRepository;
    private final StudentQuizResultRepository studentQuizResultRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public PlatformSummaryDTO getPlatformSummary() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabled(true);
        long deactivatedUsers = userRepository.countByEnabled(false);
        long students = userRepository.countByRole(Role.STUDENT);
        long teachers = userRepository.countByRole(Role.TEACHER);
        long admins = userRepository.countByRole(Role.ADMIN);

        long totalSubjects = subjectRepository.count();
        long totalChapters = chapterRepository.count();
        long totalDocs = uploadedDocumentRepository.count();
        long activeDocs = uploadedDocumentRepository.countByActive(true);

        long totalQuizzes = quizRepository.count();
        long publishedQuizzes = quizRepository.countByIsPublished(true);
        long draftQuizzes = quizRepository.countByStatus(QuizStatus.DRAFT);

        long completedAttempts = studentQuizResultRepository.count();
        long passedAttempts = studentQuizResultRepository.countByIsPassed(true);
        Double avgScore = studentQuizResultRepository.getAveragePercentage();
        double formattedAvgScore = (avgScore != null) ? Math.round(avgScore * 10.0) / 10.0 : 0.0;

        long activeEnrollments = studentEnrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE);

        return PlatformSummaryDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .deactivatedUsers(deactivatedUsers)
                .studentsCount(students)
                .teachersCount(teachers)
                .adminsCount(admins)
                .totalSubjects(totalSubjects)
                .totalChapters(totalChapters)
                .totalDocuments(totalDocs)
                .activeDocuments(activeDocs)
                .totalQuizzes(totalQuizzes)
                .publishedQuizzes(publishedQuizzes)
                .draftQuizzes(draftQuizzes)
                .completedQuizAttempts(completedAttempts)
                .passedQuizAttempts(passedAttempts)
                .averageQuizScore(formattedAvgScore)
                .totalActiveEnrollments(activeEnrollments)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserActivityTrendDTO> getUserActivityTrends() {
        // Collect actual user createdAt dates over the last 14 days via direct SQL aggregation
        LocalDate today = LocalDate.now();
        Map<String, Long> dateCounts = new TreeMap<>();

        // Initialize last 14 days with 0
        for (int i = 13; i >= 0; i--) {
            String dateStr = today.minusDays(i).format(DATE_FORMATTER);
            dateCounts.put(dateStr, 0L);
        }

        Instant startDate = today.minusDays(13).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Object[]> aggregatedTrends = userRepository.countRegistrationsByDayNative(startDate);
        for (Object[] row : aggregatedTrends) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                String dateStr = row[0].toString();
                long count = ((Number) row[1]).longValue();
                if (dateCounts.containsKey(dateStr)) {
                    dateCounts.put(dateStr, count);
                }
            }
        }

        return dateCounts.entrySet().stream()
                .map(e -> UserActivityTrendDTO.builder()
                        .date(e.getKey())
                        .registrationsCount(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseActivityDTO> getCourseActivity() {
        List<Subject> subjects = subjectRepository.findAll();
        List<CourseActivityDTO> activities = new ArrayList<>();

        for (Subject s : subjects) {
            long enrolled = studentEnrollmentRepository.countBySubjectIdAndStatus(s.getId(), EnrollmentStatus.ACTIVE);
            long chapters = chapterRepository.countBySubjectId(s.getId());
            long quizzes = quizRepository.countBySubjectId(s.getId());
            long docs = uploadedDocumentRepository.countBySubjectId(s.getId());

            activities.add(CourseActivityDTO.builder()
                    .subjectId(s.getId())
                    .subjectName(s.getName())
                    .subjectCode(s.getCode())
                    .enrolledStudentsCount(enrolled)
                    .chaptersCount(chapters)
                    .quizzesCount(quizzes)
                    .documentsCount(docs)
                    .build());
        }

        return activities;
    }
}
