# Stage 1: build the jar — runs natively on whatever machine is building it
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src ./src
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Stage 2: run the jar — this is the one that must be amd64 for Fargate
FROM --platform=linux/amd64 eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]