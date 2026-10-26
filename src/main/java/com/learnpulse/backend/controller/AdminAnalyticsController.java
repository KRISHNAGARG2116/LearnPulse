package com.learnpulse.backend.controller;

import com.learnpulse.backend.dto.*;
import com.learnpulse.backend.entity.AiTelemetryRecord;
import com.learnpulse.backend.service.AdminAnalyticsService;
import com.learnpulse.backend.service.AiTelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin Platform Analytics & AI Telemetry", description = "Privileged analytics APIs for platform metrics, user trends, course activity, and AI usage telemetry")
@SecurityRequirement(name = "bearerAuth")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;
    private final AiTelemetryService aiTelemetryService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Platform Summary", description = "High-level aggregation metrics across users, subjects, quizzes, and documents")
    public ResponseEntity<ApiResponse<PlatformSummaryDTO>> getPlatformSummary() {
        PlatformSummaryDTO summary = adminAnalyticsService.getPlatformSummary();
        return ResponseEntity.ok(ApiResponse.success("Platform summary retrieved successfully", summary));
    }

    @GetMapping("/user-activity")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get User Activity Trends", description = "Daily registration volume over the past 14 days")
    public ResponseEntity<ApiResponse<List<UserActivityTrendDTO>>> getUserActivityTrends() {
        List<UserActivityTrendDTO> trends = adminAnalyticsService.getUserActivityTrends();
        return ResponseEntity.ok(ApiResponse.success("User activity trends retrieved successfully", trends));
    }

    @GetMapping("/course-activity")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Course Activity Distribution", description = "Per-subject breakdown of chapters, quizzes, documents, and enrollments")
    public ResponseEntity<ApiResponse<List<CourseActivityDTO>>> getCourseActivity() {
        List<CourseActivityDTO> courseActivity = adminAnalyticsService.getCourseActivity();
        return ResponseEntity.ok(ApiResponse.success("Course activity retrieved successfully", courseActivity));
    }

    @GetMapping("/ai-usage")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get AI Usage Summary", description = "Aggregated telemetry metrics for AI operations, tokens, latency, and estimated costs")
    public ResponseEntity<ApiResponse<AiUsageSummaryDTO>> getAiUsageSummary() {
        AiUsageSummaryDTO aiSummary = aiTelemetryService.getAiUsageSummary();
        return ResponseEntity.ok(ApiResponse.success("AI usage summary retrieved successfully", aiSummary));
    }

    @GetMapping("/ai-telemetry")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Recent AI Telemetry Log", description = "Paginated operational log of individual AI requests")
    public ResponseEntity<ApiResponse<Page<AiTelemetryRecord>>> getAiTelemetry(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AiTelemetryRecord> telemetryPage = aiTelemetryService.getAiTelemetryPage(pageable);
        return ResponseEntity.ok(ApiResponse.success("AI telemetry records retrieved successfully", telemetryPage));
    }
}
