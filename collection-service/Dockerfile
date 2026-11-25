# syntax=docker/dockerfile:1
# Build context is the repository root (see docker-compose.yml), because every service needs the shared pom
# and the contracts module.

# ---- stage 1: build (full JDK + Maven, discarded afterwards) ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY contracts contracts
COPY collection-service collection-service
# the parent pom lists every module, so the other poms must exist (sources are not needed)
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY report-service/pom.xml report-service/pom.xml
COPY municipal-service/pom.xml municipal-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -q -pl collection-service -am package -DskipTests

# ---- stage 2: runtime (JRE only, non-root) ----
FROM eclipse-temurin:17-jre
# curl is used by the Compose healthchecks
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 app
COPY --from=build /src/collection-service/target/collection-service-*.jar /app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app.jar"]
