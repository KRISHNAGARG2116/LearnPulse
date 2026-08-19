package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.ChapterDTO;
import com.learnpulse.backend.dto.CreateChapterRequest;
import com.learnpulse.backend.entity.Chapter;
import com.learnpulse.backend.entity.Subject;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.ChapterRepository;
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
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final SubjectRepository subjectRepository;

    @Transactional
    public ChapterDTO createChapter(UUID subjectId, CreateChapterRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + subjectId));

        if (chapterRepository.existsBySubjectIdAndChapterNumber(subjectId, request.getChapterNumber())) {
            throw new ApiException("Chapter number " + request.getChapterNumber() + " already exists for subject '" + subject.getName() + "'", HttpStatus.CONFLICT);
        }

        Chapter chapter = Chapter.builder()
                .subject(subject)
                .title(request.getTitle().trim())
                .chapterNumber(request.getChapterNumber())
                .description(request.getDescription())
                .build();

        Chapter savedChapter = chapterRepository.save(chapter);
        log.info("Chapter created successfully with ID: {} for Subject ID: {}", savedChapter.getId(), subjectId);

        return mapToDTO(savedChapter);
    }

    @Transactional(readOnly = true)
    public List<ChapterDTO> getChaptersBySubject(UUID subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Subject not found with ID: " + subjectId);
        }

        return chapterRepository.findBySubjectIdOrderByChapterNumberAsc(subjectId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChapterDTO getChapterById(UUID id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with ID: " + id));
        return mapToDTO(chapter);
    }

    @Transactional
    public ChapterDTO updateChapter(UUID id, CreateChapterRequest request) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with ID: " + id));

        UUID subjectId = chapter.getSubject().getId();
        if (!chapter.getChapterNumber().equals(request.getChapterNumber()) &&
                chapterRepository.existsBySubjectIdAndChapterNumber(subjectId, request.getChapterNumber())) {
            throw new ApiException("Chapter number " + request.getChapterNumber() + " already exists for subject '" + chapter.getSubject().getName() + "'", HttpStatus.CONFLICT);
        }

        chapter.setTitle(request.getTitle().trim());
        chapter.setChapterNumber(request.getChapterNumber());
        chapter.setDescription(request.getDescription());

        Chapter updatedChapter = chapterRepository.save(chapter);
        log.info("Chapter updated successfully for ID: {}", id);

        return mapToDTO(updatedChapter);
    }

    @Transactional
    public void deleteChapter(UUID id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with ID: " + id));

        chapterRepository.delete(chapter);
        log.info("Chapter deleted successfully with ID: {}", id);
    }

    private ChapterDTO mapToDTO(Chapter chapter) {
        return ChapterDTO.builder()
                .id(chapter.getId())
                .subjectId(chapter.getSubject().getId())
                .subjectName(chapter.getSubject().getName())
                .title(chapter.getTitle())
                .chapterNumber(chapter.getChapterNumber())
                .description(chapter.getDescription())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .build();
    }
}
