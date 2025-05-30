# Build stage
FROM eclipse-temurin:17-jdk AS builder

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

# Runtime stage
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy the JAR file from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose application port
EXPOSE ${PORT}

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

#docker buildx build --platform linux/amd64,linux/arm64  --push  -t maxiplux/b2b-commerce:1.0.0   --load   -f Dockerfile .

#docker buildx build --platform linux/amd64,linux/arm64  --push  -t maxiplux/b2b-commerce:1.0.0   --load   -f Dockerfile .
