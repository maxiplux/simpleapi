# syntax=docker/dockerfile:1

# Stage 1: Build the application
FROM gradle:8.13-jdk17 AS build

WORKDIR /app

# Copy gradle configuration files and wrapper
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Make gradlew executable
RUN chmod +x ./gradlew

# Copy source code
COPY ./src ./src


# Build the application
RUN ./gradlew build -x test

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
##docker build -t maxiplux/simpleapi-client:1.0.0 .
##docker push maxiplux/simpleapi-client:1.0.0