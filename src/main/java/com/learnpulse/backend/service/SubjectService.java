package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.CreateSubjectRequest;
import com.learnpulse.backend.dto.SubjectDTO;
import com.learnpulse.backend.entity.Subject;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.SubjectRepository;
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
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Transactional
    public SubjectDTO createSubject(CreateSubjectRequest request) {
        String cleanCode = request.getCode().trim().toUpperCase();
        String cleanName = request.getName().trim();

        if (subjectRepository.existsByCode(cleanCode)) {
            throw new ApiException("Subject code '" + cleanCode + "' already exists", HttpStatus.CONFLICT);
        }

        if (subjectRepository.existsByName(cleanName)) {
            throw new ApiException("Subject name '" + cleanName + "' already exists", HttpStatus.CONFLICT);
        }

        Subject subject = Subject.builder()
                .name(cleanName)
                .code(cleanCode)
                .description(request.getDescription())
                .build();

        Subject savedSubject = subjectRepository.save(subject);
        log.info("Subject created successfully with ID: {}, Code: {}", savedSubject.getId(), savedSubject.getCode());

        return mapToDTO(savedSubject);
    }

    @Transactional(readOnly = true)
    public List<SubjectDTO> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubjectDTO getSubjectById(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));
        return mapToDTO(subject);
    }

    @Transactional
    public SubjectDTO updateSubject(UUID id, CreateSubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));

        String cleanCode = request.getCode().trim().toUpperCase();
        String cleanName = request.getName().trim();

        if (!subject.getCode().equalsIgnoreCase(cleanCode) && subjectRepository.existsByCode(cleanCode)) {
            throw new ApiException("Subject code '" + cleanCode + "' already exists", HttpStatus.CONFLICT);
        }

        if (!subject.getName().equalsIgnoreCase(cleanName) && subjectRepository.existsByName(cleanName)) {
            throw new ApiException("Subject name '" + cleanName + "' already exists", HttpStatus.CONFLICT);
        }

        subject.setName(cleanName);
        subject.setCode(cleanCode);
        subject.setDescription(request.getDescription());

        Subject updatedSubject = subjectRepository.save(subject);
        log.info("Subject updated successfully for ID: {}", id);

        return mapToDTO(updatedSubject);
    }

    @Transactional
    public void deleteSubject(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));

        subjectRepository.delete(subject);
        log.info("Subject deleted successfully with ID: {}", id);
    }

    private SubjectDTO mapToDTO(Subject subject) {
        int chapterCount = subject.getChapters() != null ? subject.getChapters().size() : 0;
        return SubjectDTO.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .description(subject.getDescription())
                .chapterCount(chapterCount)
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }
}
