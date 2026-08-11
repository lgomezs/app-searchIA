# Quarkus Hexagonal Architecture

> Production-ready reference project demonstrating **Hexagonal Architecture (Ports & Adapters)**, **DDD**, and **Quarkus 3**.

## Table of Contents

1. Overview
2. Goals
3. Architecture
4. Project Structure
5. Domain Model
6. Database Model
7. API
8. Technologies
9. Running the Project
10. Configuration
11. Testing
12. Roadmap
13. Antora Documentation

---

# 1. Overview

This project is a reference implementation of a clean enterprise architecture using Quarkus.

It demonstrates:

- Hexagonal Architecture
- Domain Driven Design
- Repository Pattern
- Use Cases
- Value Objects
- MapStruct
- Hibernate ORM

# 2. Goals

- Separate business rules from infrastructure.
- Keep the domain independent of frameworks.
- Make adapters replaceable.
- Simplify testing.
- Serve as a template for enterprise applications.

# 3. Architecture

```text
                Client
                   │
                   ▼
          REST Controller
                   │
                   ▼
            Application Layer
          (Use Cases / Services)
                   │
          Inbound / Outbound Ports
                   │
                   ▼
               Domain
        (Business Rules)
                   │
          Repository Port
                   │
                   ▼
          Persistence Adapter
             (Hibernate/JPA)
                   │
                   ▼
              PostgreSQL
```

## Layers

| Layer | Responsibility |
|-------|----------------|
| Domain | Business rules |
| Application | Use cases |
| Infrastructure | REST, DB, Kafka |
| Bootstrap | Quarkus startup |

# 4. Project Structure

```text
src
├── main
│   ├── java
│   │   ├── application
│   │   ├── domain
│   │   ├── infrastructure
│   │   └── bootstrap
│   └── resources
```

# 5. Domain Model

## Order

| Field | Description |
|-------|-------------|
| id | Identifier |
| code | Business code |
| description | Description |
| items | Order lines |

## Item

| Field | Description |
|-------|-------------|
| model | Model |
| quality | Quality |
| quantity | Quantity |

# 6. Database Model

## orders

| Column | Type |
|---------|------|
| id | INTEGER |
| code | VARCHAR |
| description | VARCHAR |

## order_lines

| Column | Type |
|---------|------|
| id | INTEGER |
| model | INTEGER |
| quality | INTEGER |
| quantity | INTEGER |
| order_id | INTEGER FK |

Relationship

```text
orders
   1
   │
   │
   *
order_lines
```

# 7. REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /orders | List orders |
| GET | /orders/{id} | Get order |
| POST | /orders | Create order |
| PUT | /orders/{id} | Update order |
| DELETE | /orders/{id} | Delete order |

## Example Request

```json
{
  "code":"ORD-001",
  "description":"Sample Order",
  "items":[
    {
      "model":10,
      "quality":1,
      "quantity":100
    }
  ]
}
```

# 8. Technologies

| Technology | Version |
|------------|---------|
| Java | 17 |
| Quarkus | 3.x |
| Hibernate ORM | Latest |
| JPA | Jakarta |
| MapStruct | 1.6.x |
| Lombok | Latest |
| Maven | 3.9+ |

# 9. Running

## Requirements

- Java 17
- Maven
- PostgreSQL

Clone

```bash
git clone https://github.com/lgomezs/quarkus-architecture-hexagonal.git
```

Build

```bash
mvn clean install
```

Run

```bash
mvn quarkus:dev
```

Swagger

```
http://localhost:8080/q/swagger-ui
```

Health

```
http://localhost:8080/q/health
```

# 10. Configuration

Example:

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/orders
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
```

# 11. Testing

```bash
mvn test
```

# 12. Roadmap

- Kafka
- Redis
- OpenTelemetry
- Prometheus
- Grafana
- Docker
- Kubernetes
- GitHub Actions
- Testcontainers
- CQRS
- Event Sourcing

# 13. Antora Documentation

This repository now contains an initial structure for publishing documentation with Antora.

How to generate the site (locally):

1. Install Antora (npm install -g @antora/cli @antora/site-generator-default)
2. Run: antora antora-playbook.yml

Recommended structure:

```text
docs/
└── modules/
    └── ROOT/
        ├── pages/
        │   ├── architecture.adoc
        │   ├── domain.adoc
        │   ├── persistence.adoc
        │   ├── api.adoc
        │   └── deployment.adoc
        ├── images/
        └── nav.adoc
```

Suggested future pages:

- Architecture Decision Records
- Sequence Diagrams
- Component Diagrams
- ER Diagram
- Deployment Guide
- Observability
- Security
- Performance
- Coding Standards

## License

MIT


