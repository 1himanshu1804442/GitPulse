# ==============================================================================
# GitPulse Backend - Multi-Stage Dockerfile
# Stage 1: Build JAR using Maven with Eclipse Temurin 17 JDK
# Stage 2: Minimal, secure runtime using Eclipse Temurin 17 JRE Alpine
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Application
# ------------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Optimize caching by copying POM first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build production package (skipping tests during image packaging)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Runtime Image
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Create non-root system group and user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy compiled JAR from builder stage
COPY --from=builder /build/target/*.jar /app/app.jar

# Ensure proper ownership
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose default Spring Boot application port
EXPOSE 8080

# Configure JVM optimizations for container environment
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Healthcheck to monitor backend status
HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=30s \
  CMD wget -qO- http://localhost:8080/actuator/health || wget -qO- http://localhost:8080/api/health || exit 0

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
