# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .
COPY src ./src
# Build the application, skipping tests to speed up deployment
RUN mvn clean package -DskipTests

# Stage 2: Create the minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a volume for uploads so they persist
VOLUME /tmp
VOLUME /app/uploads

# Copy the built jar from the build stage
COPY --from=build /app/target/safeshare-1.0.0.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the jar file, setting the active profile to 'prod'
ENTRYPOINT ["java","-jar","/app/app.jar"]
