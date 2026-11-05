# ========================================================
# LearnPulse AI LMS — Backend Multi-Stage Dockerfile
# Stage 1: Build executable JAR with Java 21 & Maven 3.9.6
# Stage 2: Minimal, secure Java 21 JRE runtime image
# ========================================================

# --- Stage 1: Build Environment ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy backend source code
COPY src ./src

# Compile and package application (skipping tests since database is not available at build time)
RUN mvn clean package -DskipTests -B

# --- Stage 2: Runtime Environment ---
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL maintainer="LearnPulse Engineering Team"
LABEL description="LearnPulse AI LMS Backend Spring Boot 3.2.5 on Java 21"
LABEL version="1.0.0"

# Install curl for health checks and troubleshooting
RUN apk add --no-cache curl

# Security: Create non-root application group and user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy packaged JAR from build stage
COPY --from=builder /build/target/learning-assistant-1.0.0-SNAPSHOT.jar app.jar

# Setup uploads directory with proper non-root ownership
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose backend application port
EXPOSE 8080

# Container-aware JVM options: 75% max RAM percentage, headless, container support
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Health check using the foundational status endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/status || exit 1

# Exec-form entrypoint to allow proper propagation of OS signals (SIGTERM/SIGINT)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
