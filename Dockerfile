FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/healthapp-backend-1.0.0.jar app.jar

EXPOSE 8080

# Add health check with better timeout and retry settings
HEALTHCHECK --interval=30s --timeout=15s --start-period=120s --retries=5 \
  CMD curl -f http://localhost:8080/api/health/simple || exit 1

# Add JVM options for better performance and stability
ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-XX:+UseG1GC", "-XX:+UseContainerSupport", "-jar", "app.jar"] 