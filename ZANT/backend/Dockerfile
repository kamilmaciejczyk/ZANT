# Multi-stage build for Spring Boot backend
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy pom.xml and download dependencies
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install Python3 and pip for ML scoring service, plus build dependencies
RUN apk add --no-cache python3 py3-pip gcc g++ musl-dev python3-dev py3-numpy py3-scipy libgomp

# Copy Python scripts and models
COPY Python/predictor.py /app/Python/predictor.py
COPY MODELS/wniosek_model.joblib /app/MODELS/wniosek_model.joblib

# Install Python dependencies for predictor
RUN pip3 install --no-cache-dir --break-system-packages joblib scikit-learn && \
    apk del gcc g++ musl-dev python3-dev

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8081

# Set environment variables for database connection
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/zant
ENV SPRING_DATASOURCE_USERNAME=zant
ENV SPRING_DATASOURCE_PASSWORD=zant123
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
