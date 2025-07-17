# Railway Dockerfile for optimized Spring Boot deployment
FROM eclipse-temurin:17-jdk-alpine as build

# Install required packages for build
RUN apk add --no-cache bash

# Set working directory
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradle gradle
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle .
COPY build.gradle .

# Make gradlew executable
RUN chmod +x ./gradlew

# Copy source code
COPY src src

# Build the application
RUN ./gradlew clean bootJar --no-daemon

# Production stage
FROM eclipse-temurin:17-jre-alpine

# Install curl for health checks (Railway compatible)
RUN apk add --no-cache curl

# Add non-root user
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring

# Set working directory
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Copy Railway environment file
COPY .env.railway .env

# Change ownership
RUN chown spring:spring app.jar .env

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# JVM optimization for Railway - allow override via environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat"

# Default profile - can be overridden by Railway environment variable
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}

# Run the application with environment variables
# Load .env file first, then Railway environment variables override
ENTRYPOINT ["sh", "-c", "set -a; [ -f .env ] && . ./.env; set +a; java ${JAVA_OPTS} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar app.jar"]
