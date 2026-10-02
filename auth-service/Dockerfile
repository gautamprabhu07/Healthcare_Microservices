FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Build context is the repository root (see docker-compose.yml).
WORKDIR /app/auth-service

COPY auth-service/pom.xml .

RUN mvn dependency:go-offline -B

COPY auth-service/src ./src

RUN mvn clean package

FROM eclipse-temurin:21-jdk AS runner

WORKDIR /app

COPY --from=builder /app/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 4005

ENTRYPOINT ["java", "-jar", "app.jar"]
