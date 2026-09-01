package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.CreateNoteRequest;
import com.learnpulse.backend.dto.DocumentDTO;
import com.learnpulse.backend.dto.NoteDTO;
import com.learnpulse.backend.entity.Chapter;
import com.learnpulse.backend.entity.Notes;
import com.learnpulse.backend.entity.Subject;
import com.learnpulse.backend.entity.UploadedDocument;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.exception.ResourceNotFoundException;
import com.learnpulse.backend.repository.ChapterRepository;
import com.learnpulse.backend.repository.NotesRepository;
import com.learnpulse.backend.repository.SubjectRepository;
import com.learnpulse.backend.repository.UploadedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotesService {

    private final NotesRepository notesRepository;
    private final DocumentService documentService;
    private final UploadedDocumentRepository documentRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;

    @Transactional
    public NoteDTO createNote(User teacher, CreateNoteRequest request, MultipartFile file) {
        Subject subject = null;
        if (request.getSubjectId() != null) {
            subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));
        }

        Chapter chapter = null;
        if (request.getChapterId() != null) {
            chapter = chapterRepository.findById(request.getChapterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with ID: " + request.getChapterId()));
        }

        UploadedDocument uploadedDocument = null;
        if (file != null && !file.isEmpty()) {
            DocumentDTO docDTO = documentService.uploadDocument(teacher, file, request.getSubjectId(), request.getChapterId());
            uploadedDocument = documentRepository.findById(docDTO.getId()).orElse(null);
        }

        Notes note = Notes.builder()
                .title(request.getTitle().trim())
                .content(request.getContent())
                .teacher(teacher)
                .subject(subject)
                .chapter(chapter)
                .document(uploadedDocument)
                .build();

        Notes savedNote = notesRepository.save(note);
        log.info("Note created successfully with ID: {} for teacher ID: {}", savedNote.getId(), teacher.getId());

        return mapToDTO(savedNote);
    }

    @Transactional(readOnly = true)
    public List<NoteDTO> getNotesByTeacher(User teacher) {
        return notesRepository.findByTeacherId(teacher.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NoteDTO getNoteById(UUID noteId) {
        Notes note = notesRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with ID: " + noteId));
        return mapToDTO(note);
    }

    private NoteDTO mapToDTO(Notes note) {
        return NoteDTO.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .teacherId(note.getTeacher().getId())
                .teacherEmail(note.getTeacher().getEmail())
                .subjectId(note.getSubject() != null ? note.getSubject().getId() : null)
                .subjectName(note.getSubject() != null ? note.getSubject().getName() : null)
                .chapterId(note.getChapter() != null ? note.getChapter().getId() : null)
                .chapterTitle(note.getChapter() != null ? note.getChapter().getTitle() : null)
                .documentId(note.getDocument() != null ? note.getDocument().getId() : null)
                .documentFileName(note.getDocument() != null ? note.getDocument().getOriginalFileName() : null)
                .createdAt(note.getCreatedAt())
                .build();
    }
}
