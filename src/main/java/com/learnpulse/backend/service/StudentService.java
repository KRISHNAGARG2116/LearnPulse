package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.StudentProfileDTO;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.entity.UserProfile;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public StudentProfileDTO getStudentProfile(User currentUser) {
        UserProfile userProfile = userProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user: " + currentUser.getEmail()));

        return mapToDTO(userProfile);
    }

    @Transactional
    public StudentProfileDTO updateStudentProfile(User currentUser, StudentProfileDTO dto) {
        UserProfile userProfile = userProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user: " + currentUser.getEmail()));

        // Security Ownership Enforcement: Only update fields for authenticated currentUser
        userProfile.setFirstName(dto.getFirstName());
        userProfile.setLastName(dto.getLastName());

        if (dto.getDepartment() != null) {
            userProfile.setDepartment(dto.getDepartment());
        }

        if (dto.getEnrollmentNumber() != null) {
            userProfile.setEnrollmentNumber(dto.getEnrollmentNumber());
        }

        UserProfile savedProfile = userProfileRepository.save(userProfile);
        log.info("Student profile updated successfully for user ID: {}", currentUser.getId());

        return mapToDTO(savedProfile);
    }

    private StudentProfileDTO mapToDTO(UserProfile profile) {
        return StudentProfileDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .email(profile.getUser().getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .department(profile.getDepartment())
                .enrollmentNumber(profile.getEnrollmentNumber())
                .build();
    }
}
