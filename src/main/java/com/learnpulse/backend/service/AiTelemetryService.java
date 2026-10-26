package com.learnpulse.backend.service;

import com.learnpulse.backend.dto.AiUsageSummaryDTO;
import com.learnpulse.backend.entity.AiTelemetryRecord;
import com.learnpulse.backend.entity.User;
import com.learnpulse.backend.repository.AiTelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiTelemetryService {

    private final AiTelemetryRepository aiTelemetryRepository;

    private static final String DEFAULT_PROVIDER = "Ollama";
    private static final String DEFAULT_MODEL = "qwen2.5-coder:7b";

    // Reference pricing rates ($ per token for cost modeling)
    private static final double INPUT_TOKEN_RATE = 0.00000015;  // $0.15 per 1M input tokens
    private static final double OUTPUT_TOKEN_RATE = 0.00000060; // $0.60 per 1M output tokens

    /**
     * Records an AI operational telemetry record with explicit token provenance.
     * Isolates any persistence errors so that telemetry issues NEVER compromise primary educational workflows.
     */
    public void recordOperation(String feature,
                                String model,
                                User user,
                                Instant startTime,
                                Instant completionTime,
                                String status,
                                Integer promptTokens,
                                Integer completionTokens,
                                String usageProvenance,
                                String errorMessage) {
        try {
            long latencyMs = Math.max(0, ChronoUnit.MILLIS.between(startTime, completionTime));

            int pTokens = (promptTokens != null) ? promptTokens : 0;
            int cTokens = (completionTokens != null) ? completionTokens : 0;
            int totTokens = pTokens + cTokens;

            String provenance = (usageProvenance != null && !usageProvenance.isBlank())
                    ? usageProvenance
                    : ((promptTokens != null || completionTokens != null) ? "LOCAL_ESTIMATION" : "UNAVAILABLE");

            // Ollama monetary API billing cost is $0.00 (local inference); simulated cloud benchmark rates document theoretical pricing
            double estimatedCost = "Ollama".equalsIgnoreCase(DEFAULT_PROVIDER)
                    ? 0.0
                    : (pTokens * INPUT_TOKEN_RATE) + (cTokens * OUTPUT_TOKEN_RATE);

            String sanitizedError = (errorMessage != null && !errorMessage.isBlank())
                    ? errorMessage.substring(0, Math.min(errorMessage.length(), 480))
                    : null;

            AiTelemetryRecord record = AiTelemetryRecord.builder()
                    .operationId(UUID.randomUUID().toString())
                    .feature(feature)
                    .provider(DEFAULT_PROVIDER)
                    .model((model != null && !model.isBlank()) ? model : DEFAULT_MODEL)
                    .userId(user != null ? user.getId() : null)
                    .userEmail(user != null ? user.getEmail() : null)
                    .startTime(startTime)
                    .completionTime(completionTime)
                    .executionLatencyMs(latencyMs)
                    .status(status)
                    .promptTokens(pTokens)
                    .completionTokens(cTokens)
                    .totalTokens(totTokens)
                    .usageProvenance(provenance)
                    .estimatedCostUsd(estimatedCost)
                    .errorMessage(sanitizedError)
                    .build();

            aiTelemetryRepository.save(record);
            log.debug("Recorded AI telemetry: feature={}, status={}, latency={}ms, tokens={}, provenance={}",
                    feature, status, latencyMs, totTokens, provenance);
        } catch (Exception e) {
            log.warn("Telemetry persistence failed (non-fatal error, AI workflow unaffected): {}", e.getMessage());
        }
    }

    /**
     * Overload for backward compatibility defaulting to LOCAL_ESTIMATION.
     */
    public void recordOperation(String feature,
                                String model,
                                User user,
                                Instant startTime,
                                Instant completionTime,
                                String status,
                                Integer promptTokens,
                                Integer completionTokens,
                                String errorMessage) {
        recordOperation(feature, model, user, startTime, completionTime, status, promptTokens, completionTokens, "LOCAL_ESTIMATION", errorMessage);
    }

    @Transactional(readOnly = true)
    public AiUsageSummaryDTO getAiUsageSummary() {
        long totalOps = aiTelemetryRepository.count();
        if (totalOps == 0) {
            return AiUsageSummaryDTO.builder()
                    .totalOperations(0)
                    .successfulOperations(0)
                    .failedOperations(0)
                    .successRatePercentage(100.0)
                    .averageLatencyMs(0.0)
                    .totalTokens(0)
                    .estimatedCostUsd(0.0)
                    .operationsByFeature(Collections.emptyMap())
                    .operationsByStatus(Collections.emptyMap())
                    .build();
        }

        List<Object[]> featureCounts = aiTelemetryRepository.countGroupedByFeature();
        Map<String, Long> byFeature = new LinkedHashMap<>();
        for (Object[] row : featureCounts) {
            byFeature.put((String) row[0], (Long) row[1]);
        }

        List<Object[]> statusCounts = aiTelemetryRepository.countGroupedByStatus();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        long successOps = 0;
        long failedOps = 0;
        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            byStatus.put(status, count);
            if ("SUCCESS".equalsIgnoreCase(status)) {
                successOps += count;
            } else {
                failedOps += count;
            }
        }

        double successRate = totalOps > 0 ? ((double) successOps / totalOps) * 100.0 : 0.0;
        Double avgLatency = aiTelemetryRepository.getAverageLatencyMs();
        Long totalTokens = aiTelemetryRepository.getTotalTokens();
        Double totalCost = aiTelemetryRepository.getTotalEstimatedCostUsd();

        return AiUsageSummaryDTO.builder()
                .totalOperations(totalOps)
                .successfulOperations(successOps)
                .failedOperations(failedOps)
                .successRatePercentage(Math.round(successRate * 10.0) / 10.0)
                .averageLatencyMs(avgLatency != null ? Math.round(avgLatency * 10.0) / 10.0 : 0.0)
                .totalTokens(totalTokens != null ? totalTokens : 0L)
                .estimatedCostUsd(totalCost != null ? Math.round(totalCost * 10000.0) / 10000.0 : 0.0)
                .operationsByFeature(byFeature)
                .operationsByStatus(byStatus)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AiTelemetryRecord> getAiTelemetryPage(Pageable pageable) {
        return aiTelemetryRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
