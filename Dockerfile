# =============================================================================
# Dockerfile — slavaro/customer-service
# Build: docker build --target prod -t slavaro/customer-service:1.0.0 .
#        docker build --target prod -t slavaro/customer-service:latest .
# Push:  docker push slavaro/customer-service:1.0.0
#        docker push slavaro/customer-service:latest
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1 — builder
# Compiles and packages the fat JAR using Maven + JDK 17.
# Nothing from this stage leaks into the final image.
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder

LABEL maintainer="slavaro@gmail.com"
LABEL org.opencontainers.image.source="https://github.com/slavaro/customer-service"
LABEL org.opencontainers.image.description="Customer management microservice"

WORKDIR /build

# Copy dependency descriptors first to exploit Docker layer caching.
# Maven dependencies are re-downloaded only when pom.xml changes.
COPY pom.xml .

# Download all dependencies in a separate layer (cached until pom.xml changes).
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B --no-transfer-progress

# Copy the rest of the source code.
COPY src ./src

# Build the fat JAR, skipping tests (tests should run in CI, not in Docker build).
RUN mvn package -B --no-transfer-progress -DskipTests

# -----------------------------------------------------------------------------
# Stage 2 — prod
# Lean JRE-only runtime image. No JDK, no Maven, no source code.
# Runs as a dedicated non-root user for security.
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS prod

# Create a dedicated non-root system user and group.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the packaged JAR from the builder stage.
COPY --from=builder /build/target/customer-service-*.jar app.jar

# Set ownership so appuser can read the JAR.
RUN chown appuser:appgroup app.jar

# Drop to non-root user before starting the process.
USER appuser

# Expose the port Spring Boot listens on.
EXPOSE 8080

# Health check — Spring Boot's /actuator/health endpoint (if actuator is on classpath).
# Falls back gracefully if actuator is not present.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"' || exit 1

# JVM tuning flags suitable for containers:
#   UseContainerSupport  — honour cgroup memory/CPU limits (default ON in JDK 17, explicit for clarity)
#   MaxRAMPercentage     — use up to 75 % of the container's RAM for the heap
#   TieredCompilation    — faster JIT warm-up
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+TieredCompilation", \
    "-jar", "app.jar"]
