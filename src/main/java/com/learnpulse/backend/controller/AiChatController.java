package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.dto.AiChatRequest;
import com.learnpulse.backend.dto.AiChatResponse;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.service.AiLlmService;
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
@Tag(name = "AI Tutor & Infrastructure", description = "AI LLM Communication & Chat History Persistence APIs")
public class AiChatController {

    private final AiLlmService aiLlmService;

    @PostMapping("/chat")
    @Operation(summary = "Academic AI Tutor Chat", description = "Dispatches student queries to local Qwen LLM via Spring AI ChatClient with multi-turn context and history persistence.")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("Received POST /api/ai/chat request from user: {}", currentUser != null ? currentUser.getEmail() : "anonymous");

        AiChatResponse response = aiLlmService.processChatRequest(request, currentUser);

        return ResponseEntity.ok(ApiResponse.success("AI response generated successfully", response));
    }
}
