package com.learnpulse.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageSummaryDTO {
    private long totalOperations;
    private long successfulOperations;
    private long failedOperations;
    private double successRatePercentage;
    private double averageLatencyMs;
    private long totalTokens;
    private double estimatedCostUsd;
    private Map<String, Long> operationsByFeature;
    private Map<String, Long> operationsByStatus;
}
