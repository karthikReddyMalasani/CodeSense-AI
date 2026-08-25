# ============================================================
# CodeSense AI — Backend Dockerfile
# ============================================================

# ─── Stage 1: Build ─────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy Maven wrapper and POM first (layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Fix Linux executable permission
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# ─── Stage 2: Runtime ───────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="CodeSense AI Team"
LABEL description="CodeSense AI Backend — IBM watsonx.ai + Spring Boot"

# Create non-root user
RUN addgroup -S codesense && adduser -S codesense -G codesense

WORKDIR /app

# Copy the fat JAR from builder
COPY --from=builder /workspace/target/*.jar app.jar

# Create upload directory
RUN mkdir -p /app/uploads && chown -R codesense:codesense /app

USER codesense

# Container health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
