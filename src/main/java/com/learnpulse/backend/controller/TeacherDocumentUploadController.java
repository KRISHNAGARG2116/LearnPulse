package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.CreateNoteRequest;
import com.learnpulse.backend.dto.DocumentDTO;
import com.learnpulse.backend.dto.NoteDTO;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.DocumentService;
import com.learnpulse.backend.service.NotesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher Material Upload Engine", description = "APIs for uploading educational documents (PDF, DOC, DOCX) and lecture notes")
public class TeacherDocumentUploadController {

    private final DocumentService documentService;
    private final NotesService notesService;

    @PostMapping(value = "/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(
            summary = "Upload Educational Document (PDF/DOC/DOCX)",
            description = "Uploads an educational document (PDF, DOC, DOCX) for a subject/chapter. Restricted to TEACHER and ADMIN roles."
    )
    public ResponseEntity<ApiResponse<DocumentDTO>> uploadDocument(
            @AuthenticationPrincipal User teacher,
            @Parameter(description = "Document file (PDF, DOC, DOCX)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional Subject ID")
            @RequestParam(value = "subjectId", required = false) UUID subjectId,
            @Parameter(description = "Optional Chapter ID")
            @RequestParam(value = "chapterId", required = false) UUID chapterId) {

        DocumentDTO documentDTO = documentService.uploadDocument(teacher, file, subjectId, chapterId);
        return new ResponseEntity<>(ApiResponse.success("Document uploaded, validated, and processed successfully", documentDTO), HttpStatus.CREATED);
    }

    @PostMapping(value = "/upload-note", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(
            summary = "Upload Lecture Note with Optional Document Attachment",
            description = "Creates a lecture note record with optional document attachment (PDF/DOC/DOCX). Restricted to TEACHER and ADMIN roles."
    )
    public ResponseEntity<ApiResponse<NoteDTO>> uploadNote(
            @AuthenticationPrincipal User teacher,
            @Parameter(description = "Lecture Note Title", required = true)
            @RequestParam("title") String title,
            @Parameter(description = "Lecture Note Text Content")
            @RequestParam(value = "content", required = false) String content,
            @Parameter(description = "Optional Subject ID")
            @RequestParam(value = "subjectId", required = false) UUID subjectId,
            @Parameter(description = "Optional Chapter ID")
            @RequestParam(value = "chapterId", required = false) UUID chapterId,
            @Parameter(description = "Optional Attachment File (PDF, DOC, DOCX)")
            @RequestParam(value = "file", required = false) MultipartFile file) {

        CreateNoteRequest request = CreateNoteRequest.builder()
                .title(title)
                .content(content)
                .subjectId(subjectId)
                .chapterId(chapterId)
                .build();

        NoteDTO noteDTO = notesService.createNote(teacher, request, file);
        return new ResponseEntity<>(ApiResponse.success("Note uploaded successfully", noteDTO), HttpStatus.CREATED);
    }
}
