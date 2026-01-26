# Stage 1: Build the JAR using Maven
FROM maven:3.6.3-openjdk-17-slim AS build
WORKDIR /app

# 1. Copy only the pom.xml first to cache dependencies (Makes builds faster!)
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copy the source code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the final lightweight production image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 3. Copy the JAR from the 'build' stage above, NOT from your local computer
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]