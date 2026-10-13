# ── Stage 1: build the jar ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies first: this layer is reused while pom.xml is unchanged.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

# Sources second: only this part reruns when the code changes.
COPY src ./src
RUN mvn -B -ntp package -DskipTests

# ── Stage 2: runtime image ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an unprivileged user, not as root.
RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /build/target/paylite-sut-*.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
