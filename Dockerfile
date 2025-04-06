# syntax=docker/dockerfile:1

# Stage 1: Build the application
FROM gradle:8.13-jdk17 AS build

# Set the working directory
WORKDIR /app

# Copy the Gradle wrapper and configuration files
COPY gradlew gradlew.bat settings.gradle build.gradle gradle/ ./

# Copy the source code
COPY src/ ./src/

# Build the application
RUN ./gradlew build --no-daemon

# Stage 2: Create the runtime image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built application from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]