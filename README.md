# Healthcare Microservices Platform

A production-style, cloud-ready backend system built to explore and demonstrate real-world **microservices architecture** using Java and Spring Boot. This is a personal project designed to showcase backend engineering skills relevant to distributed systems, event-driven design, and cloud infrastructure-as-code — the kind of architecture used in large-scale enterprise applications.

---

## Why This Project

Most portfolio projects are single monolithic apps. This one intentionally isn't — it's built as a set of independently deployable services that communicate over **REST, gRPC, and Kafka**, fronted by a gateway, secured with **JWT authentication**, and provisioned on **AWS via CDK**. It was built to practice the patterns that show up in real production systems: service isolation, inter-service communication, async messaging, infrastructure automation, and integration testing.

---

## Architecture Overview

```
                        ┌─────────────────┐
                        │   API Gateway    │  (Spring Cloud Gateway, JWT validation)
                        └────────┬─────────┘
                 ┌───────────────┼───────────────┐
                 ▼                                ▼
        ┌────────────────┐               ┌─────────────────┐
        │  Auth Service   │               │ Patient Service  │
        │ (JWT issuing)   │               │  (Core domain)   │
        └────────┬────────┘               └────────┬─────────┘
                  │                                  │ gRPC
                  │                                  ▼
                  │                        ┌───────────────────┐
                  │                        │  Billing Service   │
                  │                        └───────────────────┘
                  │                                  │ Kafka event
                  │                                  ▼
                  │                        ┌───────────────────┐
                  │                        │ Analytics Service  │
                  │                        └───────────────────┘
                  ▼
          ┌───────────────┐
          │  PostgreSQL    │ (per-service databases)
          └───────────────┘
```

Each service owns its own database and domain logic, and services never share data directly — they communicate through well-defined contracts (REST, gRPC, Kafka events).

---

## Services

| Service | Responsibility | Communication |
|---|---|---|
| **api-gateway** | Single entry point; routes requests, validates JWTs, strips/rewrites paths | Spring Cloud Gateway |
| **auth-service** | User authentication, JWT issuance & validation | REST |
| **patient-service** | Core domain service — patient CRUD, triggers billing account creation, publishes patient events | REST, gRPC client, Kafka producer |
| **billing-service** | Creates and manages billing accounts | gRPC server |
| **analytics-service** | Consumes patient events for downstream analytics | Kafka consumer |
| **infrastructure** | AWS CDK stack (VPC, RDS, ECS Fargate, MSK/Kafka, Route53 health checks) — deployable locally via LocalStack | Infrastructure-as-Code |
| **integration-tests** | End-to-end tests across service boundaries | REST-assured style integration tests |

---

## Key Features & Highlights

- **Polyglot inter-service communication** — REST for client-facing APIs, **gRPC** (with Protobuf contracts) for low-latency service-to-service calls, and **Kafka** for asynchronous, event-driven workflows.
- **JWT-based security** enforced at the gateway layer via a custom `JwtValidationGatewayFilterFactory`, so downstream services stay decoupled from auth concerns.
- **Database-per-service** pattern with independent PostgreSQL instances, avoiding tight coupling between domains.
- **Infrastructure-as-Code with AWS CDK** — the entire system (VPC, ECS Fargate cluster, RDS instances, MSK Kafka cluster, health checks, load-balanced gateway) is defined in Java and deployable to **LocalStack** for local cloud simulation, or real AWS.
- **Global exception handling & validation groups** in the patient service (e.g. distinct validation rules for create vs. update operations).
- **Dockerized services** — every service ships with its own `Dockerfile` for containerized local development and deployment.
- **Auto-generated API docs** — OpenAPI/Swagger docs are routed and exposed through the gateway per service.
- **Integration test suite** validating real cross-service flows (auth → patient) rather than only unit-level mocks.

---

## Tech Stack

- **Language / Framework:** Java 21, Spring Boot 3.4
- **Communication:** REST, gRPC (Protobuf), Apache Kafka
- **Security:** Spring Security, JWT
- **Persistence:** PostgreSQL, Spring Data JPA
- **Gateway:** Spring Cloud Gateway
- **Infrastructure:** AWS CDK (Java), Amazon ECS Fargate, RDS, MSK, Route53, LocalStack
- **Containerization:** Docker
- **Testing:** JUnit, integration tests across service boundaries

---

## Running Locally

Each service is independently buildable and runnable via its Maven wrapper:

```bash
cd patient-service
./mvnw spring-boot:run
```

To spin up the full cloud-simulated environment (VPC, databases, Kafka, ECS services) locally:

```bash
cd infrastructure
./localstack-deploy.sh
```

Sample HTTP/gRPC requests for manual testing are included under `api-requests/` and `grpc-requests/`.

---

## What This Project Demonstrates

- Designing and implementing a microservices architecture from scratch, not just consuming one.
- Comfort with multiple communication paradigms (sync REST/gRPC, async Kafka events).
- Practical, hands-on infrastructure-as-code experience (AWS CDK) rather than only clicking through a console.
- Security-conscious design (centralized JWT validation at the edge).
- Writing integration tests that validate real service collaboration.

---

## Author

Built as a personal project to deepen hands-on experience with distributed systems and cloud-native Java development.
