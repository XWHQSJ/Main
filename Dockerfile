# --- Build stage ---
FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle
COPY gradlew gradlew.bat ./
COPY src src
RUN gradle build -x test --no-daemon

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY --from=build /app/build/distributions/*.tar ./
RUN mkdir -p /out

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["zhihu", "--url", "https://www.zhihu.com/explore/recommendations", "--output", "/out/output.json", "--format", "json"]
