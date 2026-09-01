package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.DocumentDTO;
import com.learnpulse.backend.entity.*;
import com.learnpulse.backend.exception.ApiException;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.ChapterRepository;
import com.learnpulse.backend.repository.SubjectRepository;
import com.learnpulse.backend.repository.UploadedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final FileStorageService fileStorageService;
    private final DocumentTextExtractionService textExtractionService;
    private final UploadedDocumentRepository documentRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;

    public record DocumentResourceHolder(
            Resource resource,
            String originalFileName,
            String contentType
    ) {}

    @Transactional
    public DocumentDTO uploadDocument(User teacher, MultipartFile file, UUID subjectId, UUID chapterId) {
        if (teacher == null) {
            throw new ApiException("Authentication required to upload document", HttpStatus.UNAUTHORIZED);
        }

        Subject subject = null;
        if (subjectId != null) {
            subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + subjectId));
        }

        Chapter chapter = null;
        if (chapterId != null) {
            chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with ID: " + chapterId));
            if (subject != null && !chapter.getSubject().getId().equals(subject.getId())) {
                throw new ApiException("Chapter does not belong to the specified subject", HttpStatus.BAD_REQUEST);
            }
        }

        // 1. Store Physical File
        FileStorageService.StoredFileInfo storedFileInfo = fileStorageService.storeFile(file);

        // 2. Extract Document Text using Apache Tika & PDFBox
        String extractedText = "";
        ProcessingStatus status = ProcessingStatus.PROCESSED;
        try {
            extractedText = textExtractionService.extractText(
                    Paths.get(storedFileInfo.filePath()),
                    storedFileInfo.contentType(),
                    storedFileInfo.originalFileName()
            );
        } catch (Exception ex) {
            log.error("Text extraction failed for file '{}': {}", storedFileInfo.originalFileName(), ex.getMessage());
            status = ProcessingStatus.FAILED;
        }

        // 3. Persist Metadata to PostgreSQL
        try {
            UploadedDocument document = UploadedDocument.builder()
                    .originalFileName(storedFileInfo.originalFileName())
                    .storedFileName(storedFileInfo.storedFileName())
                    .filePath(storedFileInfo.filePath())
                    .fileSize(storedFileInfo.fileSize())
                    .contentType(storedFileInfo.contentType())
                    .teacher(teacher)
                    .subject(subject)
                    .chapter(chapter)
                    .active(true)
                    .processingStatus(status)
                    .extractedText(extractedText)
                    .build();

            UploadedDocument savedDoc = documentRepository.save(document);
            log.info("Document metadata saved successfully with ID: {}", savedDoc.getId());
            return mapToDTO(savedDoc);

        } catch (Exception ex) {
            log.error("Database persistence failed for uploaded file: {}. Cleaning up physical file.", storedFileInfo.storedFileName(), ex);
            fileStorageService.deleteFile(storedFileInfo.storedFileName());
            throw new ApiException("Database error saving document metadata: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public DocumentDTO getDocumentById(UUID documentId) {
        UploadedDocument doc = documentRepository.findByIdAndActiveTrue(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));
        return mapToDTO(doc);
    }

    @Transactional(readOnly = true)
    public List<UploadedDocument> getDocumentsBySubject(UUID subjectId) {
        return documentRepository.findBySubjectIdAndActiveTrue(subjectId);
    }

    @Transactional(readOnly = true)
    public DocumentResourceHolder loadDocumentResource(UUID documentId) {
        UploadedDocument doc = documentRepository.findByIdAndActiveTrue(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        Resource resource = fileStorageService.loadFileAsResource(doc.getStoredFileName());

        return new DocumentResourceHolder(
                resource,
                doc.getOriginalFileName(),
                doc.getContentType()
        );
    }

    public DocumentDTO mapToDTO(UploadedDocument doc) {
        return DocumentDTO.builder()
                .id(doc.getId())
                .originalFileName(doc.getOriginalFileName())
                .storedFileName(doc.getStoredFileName())
                .fileSize(doc.getFileSize())
                .contentType(doc.getContentType())
                .teacherId(doc.getTeacher().getId())
                .teacherEmail(doc.getTeacher().getEmail())
                .subjectId(doc.getSubject() != null ? doc.getSubject().getId() : null)
                .subjectName(doc.getSubject() != null ? doc.getSubject().getName() : null)
                .chapterId(doc.getChapter() != null ? doc.getChapter().getId() : null)
                .chapterTitle(doc.getChapter() != null ? doc.getChapter().getTitle() : null)
                .processingStatus(doc.getProcessingStatus())
                .extractedText(doc.getExtractedText())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
