package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final StudentQuizResultRepository studentQuizResultRepository;
    private final QuizRepository quizRepository;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getUsers(Role role, Boolean enabled, String search, Pageable pageable) {
        String sanitizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<User> users;
        if (sanitizedSearch != null) {
            users = userRepository.searchUsersWithKeyword(role, enabled, sanitizedSearch, pageable);
        } else if (role != null && enabled != null) {
            users = userRepository.findByRoleAndEnabled(role, enabled, pageable);
        } else if (role != null) {
            users = userRepository.findByRole(role, pageable);
        } else if (enabled != null) {
            users = userRepository.findByEnabled(enabled, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(this::mapToAdminUserDTO);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDTO getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        long enrolledSubjects = (user.getRole() == Role.STUDENT)
                ? studentEnrollmentRepository.countByStudentIdAndStatus(userId, EnrollmentStatus.ACTIVE)
                : 0L;

        long assignedSubjects = (user.getRole() == Role.TEACHER)
                ? teacherSubjectRepository.countByTeacherId(userId)
                : 0L;

        long completedQuizzes = (user.getRole() == Role.STUDENT)
                ? studentQuizResultRepository.countByStudentId(userId)
                : 0L;

        long createdQuizzes = (user.getRole() == Role.TEACHER || user.getRole() == Role.ADMIN)
                ? quizRepository.countByCreatedById(userId)
                : 0L;

        long uploadedDocs = (user.getRole() == Role.TEACHER || user.getRole() == Role.ADMIN)
                ? uploadedDocumentRepository.countByTeacherId(userId)
                : 0L;

        return AdminUserDetailDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .accountNonLocked(user.isAccountNonLocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .firstName(profile != null ? profile.getFirstName() : "")
                .lastName(profile != null ? profile.getLastName() : "")
                .department(profile != null ? profile.getDepartment() : null)
                .enrollmentNumber(profile != null ? profile.getEnrollmentNumber() : null)
                .teacherVerificationStatus(profile != null ? profile.getTeacherVerificationStatus() : null)
                .rejectionReason(profile != null ? profile.getRejectionReason() : null)
                .enrolledSubjectsCount(enrolledSubjects)
                .assignedSubjectsCount(assignedSubjects)
                .completedQuizzesCount(completedQuizzes)
                .createdQuizzesCount(createdQuizzes)
                .uploadedDocumentsCount(uploadedDocs)
                .build();
    }

    @Transactional
    public AdminUserDTO updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().user(user).build());

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            profile.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            profile.setLastName(request.getLastName().trim());
        }
        if (request.getDepartment() != null) {
            profile.setDepartment(request.getDepartment().trim());
        }
        if (request.getEnrollmentNumber() != null) {
            profile.setEnrollmentNumber(request.getEnrollmentNumber().trim());
        }

        userProfileRepository.save(profile);
        userRepository.save(user);

        log.info("Admin updated profile for user: {}", user.getEmail());
        return mapToAdminUserDTO(user);
    }

    @Transactional
    public AdminUserDTO updateUserRole(User actingAdmin, UUID targetUserId, Role newRole) {
        if (newRole == null) {
            throw new ApiException("Role cannot be null.", HttpStatus.BAD_REQUEST);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

        // Self-demotion protection
        if (actingAdmin.getId().equals(targetUserId)) {
            log.warn("Security Alert: Admin {} attempted self-role modification to {}", actingAdmin.getEmail(), newRole);
            throw new ApiException("Administrators cannot change their own role.", HttpStatus.BAD_REQUEST);
        }

        // Last-active-administrator protection (Concurrency-safe serialized row lock)
        if (targetUser.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            List<User> activeAdmins = userRepository.findActiveAdminsForUpdate(Role.ADMIN);
            if (activeAdmins.size() <= 1) {
                log.warn("Security Alert: Attempted demotion of the last active administrator: {}", targetUser.getEmail());
                throw new ApiException("Cannot demote the last remaining active administrator.", HttpStatus.CONFLICT);
            }
        }

        Role oldRole = targetUser.getRole();
        targetUser.setRole(newRole);
        User savedUser = userRepository.save(targetUser);

        log.info("Admin {} changed role of user {} from {} to {}", actingAdmin.getEmail(), targetUser.getEmail(), oldRole, newRole);
        return mapToAdminUserDTO(savedUser);
    }

    @Transactional
    public PasswordResetResponse initiatePasswordReset(User actingAdmin, UUID targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

        // Remove any prior active tokens for this user
        passwordResetTokenRepository.deleteByUser(targetUser);

        // Generate cryptographically random token
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);
        Instant expiryDate = Instant.now().plus(24, ChronoUnit.HOURS);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .user(targetUser)
                .expiryDate(expiryDate)
                .consumed(false)
                .createdByAdminId(actingAdmin.getId())
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(targetUser.getEmail(), rawToken);

        log.info("Admin {} initiated password reset for user ID {} (Token Hash: {}, Expiry: {})",
                actingAdmin.getEmail(), targetUserId, tokenHash.substring(0, 8) + "...", expiryDate);

        // Intentionally generic response - token is never returned in plaintext to prevent leak
        return PasswordResetResponse.builder()
                .message("Password reset initiated successfully. Reset token has been dispatched securely.")
                .userId(targetUserId)
                .expiryDate(expiryDate)
                .initiated(true)
                .build();
    }

    @Transactional
    public AdminUserDTO deactivateUser(User actingAdmin, UUID targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

        // Prevent self-deactivation
        if (actingAdmin.getId().equals(targetUserId)) {
            log.warn("Security Alert: Admin {} attempted self-deactivation", actingAdmin.getEmail());
            throw new ApiException("Administrators cannot deactivate their own account.", HttpStatus.BAD_REQUEST);
        }

        // Prevent deactivating last active administrator (Concurrency-safe serialized row lock)
        if (targetUser.getRole() == Role.ADMIN) {
            List<User> activeAdmins = userRepository.findActiveAdminsForUpdate(Role.ADMIN);
            if (activeAdmins.size() <= 1) {
                log.warn("Security Alert: Attempted deactivation of last active administrator: {}", targetUser.getEmail());
                throw new ApiException("Cannot deactivate the last remaining active administrator.", HttpStatus.CONFLICT);
            }
        }

        targetUser.setEnabled(false);
        User savedUser = userRepository.save(targetUser);
        log.info("Admin {} deactivated account for user: {}", actingAdmin.getEmail(), targetUser.getEmail());

        return mapToAdminUserDTO(savedUser);
    }

    @Transactional
    public AdminUserDTO reactivateUser(User actingAdmin, UUID targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

        targetUser.setEnabled(true);
        User savedUser = userRepository.save(targetUser);
        log.info("Admin {} reactivated account for user: {}", actingAdmin.getEmail(), targetUser.getEmail());

        return mapToAdminUserDTO(savedUser);
    }

    private AdminUserDTO mapToAdminUserDTO(User user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);

        return AdminUserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .accountNonLocked(user.isAccountNonLocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .firstName(profile != null ? profile.getFirstName() : "")
                .lastName(profile != null ? profile.getLastName() : "")
                .department(profile != null ? profile.getDepartment() : null)
                .enrollmentNumber(profile != null ? profile.getEnrollmentNumber() : null)
                .teacherVerificationStatus(profile != null ? profile.getTeacherVerificationStatus() : null)
                .build();
    }

    private String hashToken(String token) {
        return AuthService.hashToken(token);
    }
}
