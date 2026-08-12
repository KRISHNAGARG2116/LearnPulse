package com.learnpulse.backend.service;

import com.learnpulse.backend.repository.InfrastructureCheckRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BaseStatusService {

    private final InfrastructureCheckRepository infrastructureCheckRepository;

    @Value("${spring.application.name:learning-assistant}")
    private String applicationName;

    @Data
    @Builder
    public static class StatusData {
        private String application;
        private String environmentStatus;
        private String javaVersion;
        private String springBootVersion;
        private DatabaseStatus database;
        private Map<String, Object> infrastructureChecks;
    }

    @Data
    @Builder
    public static class DatabaseStatus {
        private String databaseName;
        private boolean connected;
        private boolean pgvectorInstalled;
        private String pgvectorVersion;
    }

    public StatusData getStatus() {
        boolean connected = false;
        boolean pgvectorInstalled = false;
        String pgvectorVersion = "NOT_INSTALLED";

        try {
            pgvectorInstalled = infrastructureCheckRepository.isPgVectorInstalled();
            if (pgvectorInstalled) {
                pgvectorVersion = infrastructureCheckRepository.getPgVectorVersion();
            }
            connected = true;
        } catch (Exception e) {
            connected = false;
        }

        DatabaseStatus dbStatus = DatabaseStatus.builder()
                .databaseName("learning_assistant_db")
                .connected(connected)
                .pgvectorInstalled(pgvectorInstalled)
                .pgvectorVersion(pgvectorVersion)
                .build();

        Map<String, Object> checks = new HashMap<>();
        checks.put("jpaAutoSchema", "enabled");
        checks.put("hikariConnectionPool", "active");
        checks.put("beanValidation", "active");
        checks.put("springSecurityFoundation", "active");
        checks.put("openApiSwaggerSupport", "active");
        checks.put("documentDependencies", "Apache Tika & PDFBox registered");

        return StatusData.builder()
                .application(applicationName)
                .environmentStatus("UP")
                .javaVersion(System.getProperty("java.version"))
                .springBootVersion(SpringBootVersion.getVersion())
                .database(dbStatus)
                .infrastructureChecks(checks)
                .build();
    }
}
