# Multi-stage build for parking-service
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built JAR
COPY --from=build /app/target/parking-service-*.jar app.jar

# Install curl and wait-for-it for health checks
RUN apk add --no-cache curl bash

# Copy docker-entrypoint script
COPY docker/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:3003/actuator/health || exit 1

EXPOSE 3003

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
