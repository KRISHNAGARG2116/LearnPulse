package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.AiTelemetryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiTelemetryRepository extends JpaRepository<AiTelemetryRecord, UUID> {

    Page<AiTelemetryRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT r.feature, COUNT(r) FROM AiTelemetryRecord r GROUP BY r.feature")
    List<Object[]> countGroupedByFeature();

    @Query("SELECT r.status, COUNT(r) FROM AiTelemetryRecord r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT AVG(r.executionLatencyMs) FROM AiTelemetryRecord r")
    Double getAverageLatencyMs();

    @Query("SELECT SUM(r.totalTokens) FROM AiTelemetryRecord r")
    Long getTotalTokens();

    @Query("SELECT SUM(r.estimatedCostUsd) FROM AiTelemetryRecord r")
    Double getTotalEstimatedCostUsd();
}
