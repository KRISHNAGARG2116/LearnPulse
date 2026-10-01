package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.DocumentRagRequest;
import com.learnpulse.backend.dto.DocumentRagResponse;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.DocumentRagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI RAG & Document Q&A", description = "Retrieval-Augmented Generation (RAG) Document Q&A Endpoint")
public class DocumentRagController {

    private final DocumentRagService documentRagService;

    @PostMapping("/ask-document")
    @Operation(
            summary = "Document-Grounded RAG Q&A",
            description = "Performs vector similarity retrieval over course document chunks and generates document-grounded answers using Qwen LLM."
    )
    public ResponseEntity<ApiResponse<DocumentRagResponse>> askDocument(
            @Valid @RequestBody DocumentRagRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("Received POST /api/ai/ask-document request for document ID {} from user: {}",
                request.getDocumentId(), currentUser != null ? currentUser.getEmail() : "anonymous");

        DocumentRagResponse response = documentRagService.askDocument(currentUser, request);

        return ResponseEntity.ok(ApiResponse.success("Document RAG response generated successfully", response));
    }
}
