# 🛒 Ecommerce Microservices

E-commerce platform built with a microservices architecture using **Java 21**, **Spring Boot 3.5**, **PostgreSQL**, and **Apache Kafka**.

---

## 🏗️ Architecture

```
Client (Browser/App)
        │
        ▼
   API Gateway  ◄──── Spring Cloud Gateway (port 8080)
        │
   ┌────┴────────────────────┐
   │                         │
   ▼                         ▼
user-service           product-service
(port 8081)            (port 8082)
   │                         │
   ▼                         ▼
PostgreSQL             PostgreSQL
(users_db)             (products_db)
        │
        ▼ (Kafka events)
   ┌────┴────────────────────┐
   │                         │
   ▼                         ▼
order-service          payment-service
(port 8083)            (port 8084)
        │
        ▼ (Kafka events)
notification-service
(port 8085)
```

### Microservices

| Service | Port | Responsibility |
|---|---|---|
| `user-service` | 8081 | Registration, login, and JWT |
| `product-service` | 8082 | Product catalog and inventory |
| `order-service` | 8083 | Order creation and management |
| `payment-service` | 8084 | Payment processing |
| `notification-service` | 8085 | Email and event-driven alerts |
| `api-gateway` | 8080 | Routing and authentication |
| `eureka-server` | 8761 | Service Discovery |

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 LTS | Core language |
| Spring Boot | 3.5.x | Microservices framework |
| Spring Cloud | 2024.x | Gateway, Eureka, Feign |
| Spring Security | 6.x | Authentication & authorization |
| Spring Data JPA | 3.x | Data persistence |
| Apache Kafka | 3.7.x | Asynchronous messaging |
| PostgreSQL | 15 | Relational database |
| Docker | 25.x | Containerization |
| Kubernetes | 1.29.x | Orchestration |
| Maven | 3.9.x | Build tool |

---

## ✅ Prerequisites

- JDK 21
- Maven 3.9+
- Docker Desktop
- Git

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Moii6/ecommerce.git
cd ecommerce
```

### 2. Start infrastructure with Docker

```bash
# Start PostgreSQL and Kafka
docker compose up -d
```

### 3. Run a service

```bash
./mvnw spring-boot:run
```

### 4. Run tests

```bash
./mvnw test
```

---

## 📡 Service Communication

- **REST (synchronous):** API Gateway → Services, and between services when an immediate response is needed via OpenFeign.
- **Kafka (asynchronous):** `order-service` publishes events → `payment-service` and `notification-service` consume them.

### Kafka Topics

| Topic | Producer | Consumers |
|---|---|---|
| `order-created` | order-service | payment-service, notification-service |
| `payment-processed` | payment-service | order-service, notification-service |

---

## 📁 Project Structure

```
ecommerce/
├── eureka-server/
├── api-gateway/
├── user-service/
├── product-service/
├── order-service/
├── payment-service/
├── notification-service/
├── docker-compose.yml
└── k8s/
    ├── deployments/
    ├── services/
    └── configmaps/
```

---

## 🔐 Environment Variables

Set the following variables before running the services. **Never commit these to the repository.**

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/your_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password
JWT_SECRET=your_jwt_secret
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

---

## 📖 Development Guide

For a detailed step-by-step implementation guide covering all phases (user-service → Kubernetes), see:

👉 [ecommerce-microservicios-guia.md](./ecommerce-microservicios-guia.md)

---

## 🗺️ Roadmap

- [x] Phase 1 — user-service (base setup, JWT authentication)
- [ ] Phase 2 — product-service (catalog and inventory)
- [ ] Phase 3 — order-service + Kafka producer
- [ ] Phase 4 — payment-service + Kafka consumer
- [ ] Phase 5 — notification-service
- [ ] Phase 6 — API Gateway
- [ ] Phase 7 — Service Discovery with Eureka
- [ ] Phase 8 — Containerization with Docker
- [ ] Phase 9 — Orchestration with Kubernetes

---

## 🤝 Contributing

Contributions are welcome! Please open an issue first to discuss any changes you'd like to make.

---

## 📄 License

This project is licensed under the MIT License.
