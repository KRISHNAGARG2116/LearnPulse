package com.learnpulse.backend.enrollment;

import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EnrollmentSchemaVerificationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private TeacherSubjectRepository teacherSubjectRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private StudentQuizResultRepository resultRepository;

    @Autowired
    private UploadedDocumentRepository documentRepository;

    @Autowired
    private NotesRepository notesRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private User teacher;
    private User student;
    private Subject subject;

    @BeforeEach
    void setUp() {
        resultRepository.deleteAll();
        questionRepository.deleteAll();
        quizRepository.deleteAll();
        notesRepository.deleteAll();
        documentRepository.deleteAll();
        studentEnrollmentRepository.deleteAll();
        teacherSubjectRepository.deleteAll();
        chapterRepository.deleteAll();
        subjectRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        teacher = userRepository.save(User.builder()
                .email("teacher_schema@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.TEACHER)
                .build());

        student = userRepository.save(User.builder()
                .email("student_schema@learnpulse.ai")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.STUDENT)
                .build());

        subject = subjectRepository.save(Subject.builder()
                .name("Database Internals")
                .code("DB601")
                .description("Storage and indexing")
                .build());
    }

    @Test
    @DisplayName("1. Database Unique Constraint uk_teacher_subject rejects duplicate teacher-subject pairs")
    void testTeacherSubjectUniqueConstraintEnforced() {
        TeacherSubject ts1 = TeacherSubject.builder()
                .teacher(teacher)
                .subject(subject)
                .build();
        teacherSubjectRepository.saveAndFlush(ts1);

        TeacherSubject ts2 = TeacherSubject.builder()
                .teacher(teacher)
                .subject(subject)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            teacherSubjectRepository.saveAndFlush(ts2);
        });
    }

    @Test
    @DisplayName("2. Database Unique Constraint uk_student_subject rejects duplicate student-subject pairs")
    void testStudentSubjectUniqueConstraintEnforced() {
        StudentEnrollment se1 = StudentEnrollment.builder()
                .student(student)
                .subject(subject)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        studentEnrollmentRepository.saveAndFlush(se1);

        StudentEnrollment se2 = StudentEnrollment.builder()
                .student(student)
                .subject(subject)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            studentEnrollmentRepository.saveAndFlush(se2);
        });
    }

    @Test
    @DisplayName("3. Soft-delete preserves StudentEnrollment row in database with status INACTIVE")
    void testSoftDeletePreservesRowWithInactiveStatus() {
        StudentEnrollment se = StudentEnrollment.builder()
                .student(student)
                .subject(subject)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        StudentEnrollment saved = studentEnrollmentRepository.saveAndFlush(se);

        // Soft-delete
        saved.setStatus(EnrollmentStatus.INACTIVE);
        studentEnrollmentRepository.saveAndFlush(saved);

        // Verify row still exists in database
        StudentEnrollment fromDb = studentEnrollmentRepository.findById(saved.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals(EnrollmentStatus.INACTIVE, fromDb.getStatus());
        assertEquals(1, studentEnrollmentRepository.count());
    }

    @Test
    @DisplayName("4. Soft-delete unenrollment preserves student quiz results and history intact")
    void testSoftDeletePreservesStudentQuizResults() {
        // Create Quiz
        Quiz quiz = quizRepository.save(Quiz.builder()
                .title("DB Indexing Quiz")
                .subject(subject)
                .quizType(QuizType.FINAL_COURSE_QUIZ)
                .status(QuizStatus.PUBLISHED)
                .isPublished(true)
                .createdBy(teacher)
                .totalMarks(10)
                .passingScorePercentage(75.0)
                .build());

        // Create Quiz Result for student
        StudentQuizResult result = resultRepository.save(StudentQuizResult.builder()
                .student(student)
                .quiz(quiz)
                .score(9)
                .totalMarks(10)
                .correctAnswers(9)
                .wrongAnswers(1)
                .percentage(90.0)
                .isPassed(true)
                .build());

        // Enroll and then unenroll (soft delete)
        StudentEnrollment se = studentEnrollmentRepository.save(StudentEnrollment.builder()
                .student(student)
                .subject(subject)
                .status(EnrollmentStatus.ACTIVE)
                .build());

        se.setStatus(EnrollmentStatus.INACTIVE);
        studentEnrollmentRepository.saveAndFlush(se);

        // Historical quiz result must NOT be deleted
        StudentQuizResult preservedResult = resultRepository.findById(result.getId()).orElse(null);
        assertNotNull(preservedResult);
        assertEquals(90.0, preservedResult.getPercentage());
        assertTrue(Boolean.TRUE.equals(preservedResult.getIsPassed()));
        assertEquals(student.getId(), preservedResult.getStudent().getId());
    }
}
