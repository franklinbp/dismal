# Stage 1: Build the application
FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN chmod +x mvnw

# Download dependencies to improve layer caching in subsequent builds
COPY pom.xml .
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw -B -Dmaven.test.skip=true package # -B for batch mode

# Stage 2: Create the production image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-privileged user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the executable Spring Boot JAR without coupling the image to the Maven artifact name.
COPY --from=build --chown=appuser:appgroup /workspace/target/*.jar /app/app.jar

# Switch to the non-privileged user
USER appuser

EXPOSE 8080

# Healthcheck using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget -q -O- http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
