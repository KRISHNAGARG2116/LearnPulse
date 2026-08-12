package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.ApiResponse;
import com.learnpulse.backend.service.BaseStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
@RequiredArgsConstructor
@Tag(name = "Infrastructure Status", description = "Foundational verification endpoint for Week 2 backend infrastructure")
public class BaseStatusController {

    private final BaseStatusService baseStatusService;

    @GetMapping
    @Operation(summary = "Get Backend Infrastructure Status", description = "Verifies Spring Boot startup, Java 21, PostgreSQL connectivity, and pgvector extension availability")
    public ResponseEntity<ApiResponse<BaseStatusService.StatusData>> getStatus() {
        BaseStatusService.StatusData statusData = baseStatusService.getStatus();
        ApiResponse<BaseStatusService.StatusData> response = ApiResponse.success(
                "Backend infrastructure operating successfully",
                statusData
        );
        return ResponseEntity.ok(response);
    }
}
