# Multi-stage build for Spring Boot backend
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Set environment variables for database connection
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/zant
ENV SPRING_DATASOURCE_USERNAME=zant
ENV SPRING_DATASOURCE_PASSWORD=zant123
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
