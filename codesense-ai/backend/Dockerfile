# ============================================================
# CodeSense AI — Backend Dockerfile
# ============================================================

# ─── Stage 1: Build ─────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /workspace

# Copy POM first for better Docker layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source
COPY src/ src/

# Build Spring Boot application
RUN mvn package -DskipTests -B

# ─── Stage 2: Runtime ───────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="CodeSense AI Team"
LABEL description="CodeSense AI Backend — Spring Boot"

# Create non-root user
RUN addgroup -S codesense && adduser -S codesense -G codesense

WORKDIR /app

# Copy generated JAR
COPY --from=builder /workspace/target/*.jar app.jar

# Create upload directory and set permissions
RUN mkdir -p /app/uploads && \
    chown -R codesense:codesense /app

USER codesense

# Render/Spring Boot health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
