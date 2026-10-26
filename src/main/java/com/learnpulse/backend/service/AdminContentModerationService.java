package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.QuizRepository;
import com.learnpulse.backend.repository.UploadedDocumentRepository;
import com.learnpulse.backend.repository.UserProfileRepository;
import com.learnpulse.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminContentModerationService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UploadedDocumentRepository documentRepository;
    private final QuizRepository quizRepository;

    @Transactional(readOnly = true)
    public List<TeacherVerificationDTO> getTeachers(TeacherVerificationStatus statusFilter) {
        List<User> teachers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TEACHER)
                .collect(Collectors.toList());

        return teachers.stream()
                .map(t -> {
                    UserProfile profile = userProfileRepository.findByUserId(t.getId()).orElse(null);
                    TeacherVerificationStatus status = (profile != null && profile.getTeacherVerificationStatus() != null)
                            ? profile.getTeacherVerificationStatus()
                            : TeacherVerificationStatus.APPROVED; // Existing or default approved

                    return TeacherVerificationDTO.builder()
                            .userId(t.getId())
                            .email(t.getEmail())
                            .firstName(profile != null ? profile.getFirstName() : "")
                            .lastName(profile != null ? profile.getLastName() : "")
                            .department(profile != null ? profile.getDepartment() : null)
                            .status(status)
                            .rejectionReason(profile != null ? profile.getRejectionReason() : null)
                            .registeredAt(t.getCreatedAt())
                            .build();
                })
                .filter(dto -> statusFilter == null || dto.getStatus() == statusFilter)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeacherVerificationDTO approveTeacher(UUID teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + teacherId));

        if (teacher.getRole() != Role.TEACHER) {
            throw new ApiException("User is not a teacher (role: " + teacher.getRole() + ")", HttpStatus.BAD_REQUEST);
        }

        UserProfile profile = userProfileRepository.findByUserId(teacherId)
                .orElseGet(() -> UserProfile.builder().user(teacher).build());

        profile.setTeacherVerificationStatus(TeacherVerificationStatus.APPROVED);
        profile.setRejectionReason(null);
        userProfileRepository.save(profile);

        log.info("Admin verified and approved faculty member: {}", teacher.getEmail());

        return TeacherVerificationDTO.builder()
                .userId(teacher.getId())
                .email(teacher.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .department(profile.getDepartment())
                .status(TeacherVerificationStatus.APPROVED)
                .rejectionReason(null)
                .registeredAt(teacher.getCreatedAt())
                .build();
    }

    @Transactional
    public TeacherVerificationDTO rejectTeacher(UUID teacherId, String reason) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + teacherId));

        if (teacher.getRole() != Role.TEACHER) {
            throw new ApiException("User is not a teacher (role: " + teacher.getRole() + ")", HttpStatus.BAD_REQUEST);
        }

        UserProfile profile = userProfileRepository.findByUserId(teacherId)
                .orElseGet(() -> UserProfile.builder().user(teacher).build());

        profile.setTeacherVerificationStatus(TeacherVerificationStatus.REJECTED);
        profile.setRejectionReason(reason != null ? reason.trim() : "Documentation verification requirements not met");
        userProfileRepository.save(profile);

        log.info("Admin rejected faculty verification for: {} (reason: {})", teacher.getEmail(), reason);

        return TeacherVerificationDTO.builder()
                .userId(teacher.getId())
                .email(teacher.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .department(profile.getDepartment())
                .status(TeacherVerificationStatus.REJECTED)
                .rejectionReason(profile.getRejectionReason())
                .registeredAt(teacher.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminDocumentDTO> getDocuments(Boolean activeFilter) {
        List<UploadedDocument> docs = documentRepository.findAll();

        return docs.stream()
                .filter(d -> activeFilter == null || d.isActive() == activeFilter)
                .map(d -> AdminDocumentDTO.builder()
                        .id(d.getId())
                        .originalFileName(d.getOriginalFileName())
                        .fileSize(d.getFileSize())
                        .contentType(d.getContentType())
                        .teacherEmail(d.getTeacher() != null ? d.getTeacher().getEmail() : "N/A")
                        .subjectName(d.getSubject() != null ? d.getSubject().getName() : "General")
                        .active(d.isActive())
                        .processingStatus(d.getProcessingStatus())
                        .createdAt(d.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminDocumentDTO updateDocumentStatus(UUID documentId, Boolean active) {
        UploadedDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        if (active != null) {
            doc.setActive(active);
        }
        UploadedDocument saved = documentRepository.save(doc);

        log.info("Admin updated document '{}' active status to {}", doc.getOriginalFileName(), doc.isActive());

        return AdminDocumentDTO.builder()
                .id(saved.getId())
                .originalFileName(saved.getOriginalFileName())
                .fileSize(saved.getFileSize())
                .contentType(saved.getContentType())
                .teacherEmail(saved.getTeacher() != null ? saved.getTeacher().getEmail() : "N/A")
                .subjectName(saved.getSubject() != null ? saved.getSubject().getName() : "General")
                .active(saved.isActive())
                .processingStatus(saved.getProcessingStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminQuizDTO> getQuizzes(QuizStatus statusFilter) {
        List<Quiz> quizzes = quizRepository.findAll();

        return quizzes.stream()
                .filter(q -> statusFilter == null || q.getStatus() == statusFilter)
                .map(q -> AdminQuizDTO.builder()
                        .id(q.getId())
                        .title(q.getTitle())
                        .subjectName(q.getSubject() != null ? q.getSubject().getName() : "General")
                        .status(q.getStatus())
                        .isPublished(q.getIsPublished())
                        .totalMarks(q.getTotalMarks())
                        .creatorEmail(q.getCreatedBy() != null ? q.getCreatedBy().getEmail() : "N/A")
                        .createdAt(q.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminQuizDTO updateQuizStatus(UUID quizId, QuizStatus status, Boolean isPublished) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with ID: " + quizId));

        if (status != null) {
            quiz.setStatus(status);
        }
        if (isPublished != null) {
            quiz.setIsPublished(isPublished);
        }
        Quiz saved = quizRepository.save(quiz);

        log.info("Admin updated quiz '{}' status to {} (published: {})", quiz.getTitle(), quiz.getStatus(), quiz.getIsPublished());

        return AdminQuizDTO.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .subjectName(saved.getSubject() != null ? saved.getSubject().getName() : "General")
                .status(saved.getStatus())
                .isPublished(saved.getIsPublished())
                .totalMarks(saved.getTotalMarks())
                .creatorEmail(saved.getCreatedBy() != null ? saved.getCreatedBy().getEmail() : "N/A")
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
