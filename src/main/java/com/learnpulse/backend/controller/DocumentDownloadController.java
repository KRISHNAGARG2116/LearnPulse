package com.learnpulse.backend.controller;

import com.learnpulse.backend.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document Retrieval Engine", description = "REST APIs for secure document download and streaming")
public class DocumentDownloadController {

    private final DocumentService documentService;

    @GetMapping("/{documentId}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download Document", description = "Downloads an uploaded educational document as an attachment")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID documentId) {
        DocumentService.DocumentResourceHolder holder = documentService.loadDocumentResource(documentId);

        String contentType = holder.contentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + holder.originalFileName() + "\"")
                .body(holder.resource());
    }

    @GetMapping("/{documentId}/stream")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Stream Document", description = "Streams an uploaded educational document inline (e.g. for PDF viewer)")
    public ResponseEntity<Resource> streamDocument(@PathVariable UUID documentId) {
        DocumentService.DocumentResourceHolder holder = documentService.loadDocumentResource(documentId);

        String contentType = holder.contentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + holder.originalFileName() + "\"")
                .body(holder.resource());
    }
}
