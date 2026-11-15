# syntax=docker/dockerfile:1
# Multi-stage build. Build context: the repository root (the BPMN models in bpmn-models/ are packaged into the jar).

# ---- 1. Build stage: Maven + JDK 17, dependencies cached in a separate layer
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace/backend-springboot
COPY backend-springboot/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY backend-springboot/src ./src
COPY bpmn-models /workspace/bpmn-models
RUN mvn -q -B -DskipTests package && cp target/event-team-registration.jar /workspace/app.jar

# ---- 2. Runtime stage: JRE only (multi-arch: amd64 and arm64), non-root user
FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER app
EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=12 \
  CMD curl -fs http://localhost:8081/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
