package com.learnpulse.backend.security;

import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.TeacherVerificationStatus;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.entity.UserProfile;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component("teacherVerificationGuard")
@RequiredArgsConstructor
@Slf4j
public class TeacherVerificationGuard {

    private final UserProfileRepository userProfileRepository;

    /**
     * SpEL-friendly evaluator for method security.
     */
    public boolean isVerifiedTeacher(User user) {
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (user.getRole() != Role.TEACHER) {
            return false;
        }

        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            return true; // legacy unmigrated account
        }

        TeacherVerificationStatus status = profile.getTeacherVerificationStatus();
        return status == null || status == TeacherVerificationStatus.APPROVED;
    }

    /**
     * Programmatic enforcement that throws a descriptive HTTP 403 Forbidden ApiException.
     */
    public void checkVerifiedTeacher(User user) {
        if (user == null) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        if (user.getRole() == Role.ADMIN) {
            return; // Admins bypass faculty verification
        }
        if (user.getRole() != Role.TEACHER) {
            throw new ApiException("Access Denied: Requires TEACHER role", HttpStatus.FORBIDDEN);
        }

        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            return; // legacy unmigrated account
        }

        TeacherVerificationStatus status = profile.getTeacherVerificationStatus();
        if (status == TeacherVerificationStatus.PENDING) {
            log.warn("Access Denied: Teacher {} attempted protected action with PENDING status", user.getEmail());
            throw new ApiException("Teacher account verification is pending administrator approval.", HttpStatus.FORBIDDEN);
        }
        if (status == TeacherVerificationStatus.REJECTED) {
            String reason = (profile.getRejectionReason() != null && !profile.getRejectionReason().isBlank())
                    ? " Reason: " + profile.getRejectionReason()
                    : "";
            log.warn("Access Denied: Teacher {} attempted protected action with REJECTED status", user.getEmail());
            throw new ApiException("Teacher account verification was rejected." + reason, HttpStatus.FORBIDDEN);
        }
    }
}
