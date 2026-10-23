
# STEP 1: BUILD SPRING BOOT
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copy Maven Wrapper
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Give permission to execute Maven Wrapper
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy Spring Boot source code
COPY src/ src/

# Build the Spring Boot application
RUN ./mvnw clean package -DskipTests

# STEP 2: RUN SPRING BOOT
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the compiled Spring Boot JAR
COPY --from=build /app/target/*.jar app.jar

# Spring Boot port
EXPOSE 8080

# Start Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
