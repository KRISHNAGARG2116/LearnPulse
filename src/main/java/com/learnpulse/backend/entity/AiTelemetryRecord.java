package com.learnpulse.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_telemetry_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTelemetryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "operation_id", nullable = false)
    private String operationId;

    @Column(name = "feature", nullable = false)
    private String feature;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "completion_time", nullable = false)
    private Instant completionTime;

    @Column(name = "execution_latency_ms", nullable = false)
    private Long executionLatencyMs;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "usage_provenance", nullable = false)
    private String usageProvenance;

    @Column(name = "estimated_cost_usd")
    private Double estimatedCostUsd;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
