package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.TeacherProfileDTO;
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
public class TeacherService {

    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public TeacherProfileDTO getTeacherProfile(User currentUser) {
        UserProfile userProfile = userProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));

        return mapToDTO(userProfile);
    }

    @Transactional
    public TeacherProfileDTO updateTeacherProfile(User currentUser, TeacherProfileDTO dto) {
        UserProfile userProfile = userProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + currentUser.getEmail()));

        // Security Ownership Enforcement: Only update fields for authenticated currentUser
        userProfile.setFirstName(dto.getFirstName());
        userProfile.setLastName(dto.getLastName());

        if (dto.getDepartment() != null) {
            userProfile.setDepartment(dto.getDepartment());
        }

        UserProfile savedProfile = userProfileRepository.save(userProfile);
        log.info("Teacher profile updated successfully for user ID: {}", currentUser.getId());

        return mapToDTO(savedProfile);
    }

    private TeacherProfileDTO mapToDTO(UserProfile profile) {
        return TeacherProfileDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .email(profile.getUser().getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .department(profile.getDepartment())
                .build();
    }
}
