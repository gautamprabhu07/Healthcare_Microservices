FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Build context is the repository root (see docker-compose.yml).
WORKDIR /app/analytics-service

COPY analytics-service/pom.xml .

RUN mvn dependency:go-offline -B

COPY contracts /app/contracts

COPY analytics-service/src ./src

RUN mvn clean package

FROM eclipse-temurin:21-jdk AS runner

WORKDIR /app

COPY --from=builder /app/analytics-service/target/analytics-service-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 4002

ENTRYPOINT ["java", "-jar", "app.jar"]
