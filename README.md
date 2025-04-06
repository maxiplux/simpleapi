# Running the Project with Docker

This section provides instructions to build and run the project using Docker.

## Prerequisites

- Ensure Docker and Docker Compose are installed on your system.
- The project requires the following Docker images:
  - `gradle:8.13-jdk17` for building the application.
  - `openjdk:17-jdk-slim` for running the application.

## Build and Run Instructions

1. Clone the repository and navigate to the project root directory.
2. Build and start the application using Docker Compose:

   ```bash
   docker-compose up --build
   ```

3. Access the application at `http://localhost:8080`.

## Configuration

- The application exposes port `8080` for HTTP traffic.
- Modify the `docker-compose.yml` file to adjust configurations as needed.

## Notes

- Ensure any required environment variables are set in a `.env` file if needed.
- The application is configured to restart automatically unless stopped manually.

For further details, refer to the existing documentation or contact the project maintainers.