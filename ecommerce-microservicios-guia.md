# Guía de Desarrollo: E-Commerce con Microservicios

**Stack empresarial moderno con Spring Boot, Kafka, Docker y Kubernetes**

---

## Cómo usar esta guía

Esta guía está diseñada para avanzar paso a paso en la construcción de un ecosistema de microservicios. Cada fase incluye:

- Objetivo y alcance
- Dependencias recomendadas
- Endpoints clave
- Casos de uso y pruebas iniciales
- Notas de integración y despliegue

Puedes usarla como un plan de trabajo: primero completa `user-service`, luego `product-service`, y así sucesivamente.

---

## Índice

1. [Arquitectura General](#1-arquitectura-general)
2. [Configuración del Entorno](#2-configuración-del-entorno)
3. [Roadmap de desarrollo](#3-roadmap-de-desarrollo)
4. [Fase 1 — user-service](#4-fase-1--user-service)
5. [Fase 2 — product-service](#5-fase-2--product-service)
6. [Fase 3 — order-service](#6-fase-3--order-service)
7. [Fase 4 — payment-service](#7-fase-4--payment-service)
8. [Fase 5 — notification-service](#8-fase-5--notification-service)
9. [Fase 6 — API Gateway](#9-fase-6--api-gateway)
10. [Fase 7 — Service Discovery con Eureka](#10-fase-7--service-discovery-con-eureka)
11. [Fase 8 — Comunicación Asíncrona con Kafka](#11-fase-8--comunicación-asíncrona-con-kafka)
12. [Fase 9 — Contenerización con Docker](#12-fase-9--contenerización-con-docker)
13. [Fase 10 — Orquestación con Kubernetes](#13-fase-10--orquestación-con-kubernetes)
14. [Checklist general de desarrollo](#15-checklist-general-de-desarrollo)
15. [Tecnologías y Versiones](#16-tecnologías-y-versiones)

---

## 1. Arquitectura General

### Diagrama de servicios

```
Cliente (Browser/App)
        │
        ▼
   API Gateway  ◄──── Spring Cloud Gateway (puerto 8080)
        │
   ┌────┴────────────────────┐
   │                         │
   ▼                         ▼
user-service           product-service
(puerto 8081)          (puerto 8082)
   │                         │
   ▼                         ▼
PostgreSQL             PostgreSQL
(users_db)             (products_db)
        │
        ▼ (eventos Kafka)
   ┌────┴────────────────────┐
   │                         │
   ▼                         ▼
order-service          payment-service
(puerto 8083)          (puerto 8084)
   │                         │
   ▼                         ▼
PostgreSQL             PostgreSQL
(orders_db)            (payments_db)
        │
        ▼ (eventos Kafka)
notification-service
(puerto 8085)
```

### Tabla de microservicios

| Servicio             | Puerto | Responsabilidad              | Base de Datos |
| -------------------- | ------ | ---------------------------- | ------------- |
| user-service         | 8081   | Registro, login, JWT         | PostgreSQL    |
| product-service      | 8082   | Catálogo, inventario         | PostgreSQL    |
| order-service        | 8083   | Crear y gestionar órdenes    | PostgreSQL    |
| payment-service      | 8084   | Procesar pagos               | PostgreSQL    |
| notification-service | 8085   | Emails y alertas             | Sin BD        |
| api-gateway          | 8080   | Enrutamiento y autenticación | Sin BD        |
| eureka-server        | 8761   | Service Discovery            | Sin BD        |

### Patrones de comunicación

- **REST (síncrono):** API Gateway → Servicios, y entre servicios cuando se requiere respuesta inmediata
- **Kafka (asíncrono):** order-service publica eventos → payment-service y notification-service consumen

---

## 2. Configuración del Entorno

### Requisitos previos

- JDK 21
- Maven 3.9+
- Docker Desktop
- VS Code con Extension Pack for Java y Spring Boot Extension Pack
- Git

### Generar un nuevo proyecto desde VS Code

1. Abre VS Code y asegúrate de que la extensión **Spring Boot Extension Pack** esté instalada.
2. Abre la paleta de comandos con `Cmd+Shift+P`.
3. Escribe y selecciona `Spring Initializr: Generate a Maven Project`.
4. Completa los datos:
   - Lenguaje: `Java`
   - Grupo: `com.example`
   - Artefacto: `user-service` (o `product-service`, etc.)
   - Versión de Spring Boot: la recomendada por la extensión
   - Dependencias: `Spring Web`, `Spring Data JPA`, `PostgreSQL Driver`, `Spring Security`, `Lombok`, `Spring Boot DevTools`, `Validation`, `Eureka Discovery Client`
5. Elige una carpeta de destino para el proyecto.
6. Abre la carpeta generada en VS Code.

> Alternativa: si no quieres usar la paleta, también puedes generar el proyecto en la web de Spring Initializr y luego importarlo en VS Code.

### Estructura de carpetas del proyecto

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

### Crear la carpeta raíz del proyecto

```bash
mkdir ~/projects/ecommerce && cd ~/projects/ecommerce
```

---

## 3. Roadmap de desarrollo

Esta guía está organizada para construir el sistema en fases incrementales. El orden recomendado es:

1. `user-service` — base de usuarios, autenticación y JWT.
2. `product-service` — catálogo de productos e inventario.
3. `order-service` — creación de pedidos y validación de stock.
4. `payment-service` — procesamiento de pagos y eventos de pago.
5. `notification-service` — notificaciones por eventos.
6. `api-gateway` — enrutamiento, seguridad y borde de entrada.
7. `eureka-server` — discovery de servicios.
8. Kafka — mensajería asíncrona entre servicios.
9. Docker — contenerización de cada servicio.
10. Kubernetes — despliegue en clúster.

Cada fase debe ser probada de forma independiente primero, y luego integrada con la anterior.

### Comandos básicos por fase

- Compilar y ejecutar un servicio:
  ```bash
  ./mvnw clean package && ./mvnw spring-boot:run
  ```
- Ejecutar pruebas unitarias:
  ```bash
  ./mvnw test
  ```
- Construir imagen Docker:
  ```bash
  docker build -t <service-name> .
  ```
- Correr un servicio local con Docker:
  ```bash
  docker run --rm -p <port>:<port> <service-name>
  ```
- Levantar Kafka local con Docker Compose:
  ```bash
  docker compose -f docker-compose-kafka.yml up -d
  ```

---

## 4. Fase 1 — user-service

### Objetivo

Implementar registro y login de usuarios con autenticación JWT.

### Dependencias (Spring Initializr)

- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL Driver
- Lombok
- Spring Boot DevTools
- Validation
- Eureka Discovery Client

### Funcionalidades a implementar

1. **Modelo:** entidad `User` con campos id, nombre, email, password, rol
2. **Repositorio:** `UserRepository` extendiendo `JpaRepository`
3. **Servicio:** `UserService` con métodos register y login
4. **Seguridad:** configuración de Spring Security con filtro JWT
5. **JWT Utility:** clase para generar y validar tokens
6. **Controlador:** endpoints REST

### Endpoints

| Método | Endpoint            | Descripción        | Autenticación |
| ------ | ------------------- | ------------------ | ------------- |
| POST   | /api/users/register | Registrar usuario  | No            |
| POST   | /api/users/login    | Login, retorna JWT | No            |
| GET    | /api/users/profile  | Perfil del usuario | Sí (JWT)      |
| GET    | /api/users/{id}     | Obtener usuario    | Sí (JWT)      |

### Configuración application.yml

```yaml
server:
  port: 8081

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:postgresql://localhost:5432/users_db
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: your-secret-key
  expiration: 86400000 # 24 horas

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Conceptos a aprender en esta fase

- Spring Security filter chain
- JWT (generación, validación, claims)
- BCrypt para encriptación de contraseñas
- JPA y manejo de entidades
- Inyección de dependencias con `@Autowired` y `@Service`

### Paquetes recomendados

- `com.example.userservice.model`
- `com.example.userservice.repository`
- `com.example.userservice.service`
- `com.example.userservice.controller`
- `com.example.userservice.security`
- `com.example.userservice.dto`

### Componentes clave

- `User` entidad con `id`, `name`, `email`, `password`, `role`
- `UserRepository extends JpaRepository<User, Long>`
- `UserService` para registrar y autenticar usuarios
- `JwtUtils` para generar/validar tokens
- `JwtAuthenticationFilter` para validar bearer token en cada request
- `SecurityConfig` para permitir registro/login sin token y proteger el resto de endpoints

### Ejemplo de endpoint de prueba

```bash
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Juan","email":"juan@mail.com","password":"secret"}'
```

```bash
curl -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"juan@mail.com","password":"secret"}'
```

### Checklist de user-service

- [ ] Crear entidad `User` y repositorio JPA
- [ ] Implementar servicio de registro
- [ ] Implementar servicio de login con JWT
- [ ] Configurar Spring Security y filtros JWT
- [ ] Crear endpoints públicos `register` y `login`
- [ ] Crear endpoint privado `profile` protegido por JWT
- [ ] Probar con `curl` o Postman
- [ ] Ejecutar `./mvnw test`

---

## 5. Fase 2 — product-service

### Objetivo

Gestionar el catálogo de productos e inventario.

### Dependencias (Spring Initializr)

- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Lombok
- Validation
- Eureka Discovery Client

### Funcionalidades a implementar

1. **Modelo:** entidad `Product` con campos id, nombre, descripción, precio, stock, categoría
2. **Repositorio:** `ProductRepository` con queries personalizados
3. **Servicio:** `ProductService` con CRUD completo y búsqueda por categoría
4. **Controlador:** endpoints REST protegidos con JWT (via gateway)

### Endpoints

| Método | Endpoint                     | Descripción            |
| ------ | ---------------------------- | ---------------------- |
| GET    | /api/products                | Listar todos           |
| GET    | /api/products/{id}           | Obtener producto       |
| GET    | /api/products/category/{cat} | Filtrar por categoría  |
| POST   | /api/products                | Crear producto (admin) |
| PUT    | /api/products/{id}           | Actualizar producto    |
| DELETE | /api/products/{id}           | Eliminar producto      |
| PATCH  | /api/products/{id}/stock     | Actualizar stock       |

### Conceptos a aprender en esta fase

- Queries personalizados con `@Query` en JPA
- DTOs (Data Transfer Objects) y MapStruct
- Paginación con `Pageable`
- Validaciones con `@Valid` y anotaciones de Bean Validation
- Manejo de excepciones con `@ControllerAdvice`

---

## 6. Fase 3 — order-service

### Objetivo

Crear y gestionar órdenes de compra. Publicar eventos a Kafka cuando se crea una orden.

### Dependencias (Spring Initializr)

- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Lombok
- Spring for Apache Kafka
- Eureka Discovery Client
- OpenFeign (para llamar a product-service)

### Funcionalidades a implementar

1. **Modelo:** entidades `Order` y `OrderItem`
2. **Comunicación síncrona:** llamar a product-service via Feign para verificar stock
3. **Comunicación asíncrona:** publicar evento `OrderCreatedEvent` a Kafka
4. **Saga pattern básico:** manejo de compensación si el pago falla

### Endpoints

| Método | Endpoint                | Descripción         |
| ------ | ----------------------- | ------------------- |
| POST   | /api/orders             | Crear orden         |
| GET    | /api/orders/{id}        | Obtener orden       |
| GET    | /api/orders/user/{id}   | Órdenes del usuario |
| PATCH  | /api/orders/{id}/status | Actualizar estado   |

### Evento Kafka publicado

```json
{
  "orderId": "uuid",
  "userId": "uuid",
  "totalAmount": 150.00,
  "items": [...],
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### Conceptos a aprender en esta fase

- OpenFeign para comunicación entre servicios
- Apache Kafka Producer
- Diseño de eventos y schema de mensajes
- Transacciones distribuidas y Saga pattern
- Estados de una orden (PENDING, CONFIRMED, CANCELLED, DELIVERED)

---

## 7. Fase 4 — payment-service

### Objetivo

Consumir eventos de Kafka del order-service y procesar pagos simulados.

### Dependencias (Spring Initializr)

- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Lombok
- Spring for Apache Kafka
- Eureka Discovery Client

### Funcionalidades a implementar

1. **Kafka Consumer:** escuchar eventos `OrderCreatedEvent`
2. **Procesamiento:** simular pago y registrar resultado
3. **Publicar resultado:** enviar evento `PaymentProcessedEvent` a Kafka
4. **Manejo de errores:** Dead Letter Queue para mensajes fallidos

### Endpoints

| Método | Endpoint                | Descripción        |
| ------ | ----------------------- | ------------------ |
| GET    | /api/payments/{orderId} | Estado del pago    |
| GET    | /api/payments/user/{id} | Historial de pagos |

### Conceptos a aprender en esta fase

- Apache Kafka Consumer y Consumer Groups
- Dead Letter Queue (DLQ)
- Idempotencia en procesamiento de eventos
- Retry policies con Spring Kafka

---

## 8. Fase 5 — notification-service

### Objetivo

Consumir eventos de Kafka y enviar notificaciones (email simulado en consola).

### Dependencias (Spring Initializr)

- Spring Web
- Spring for Apache Kafka
- Lombok
- Eureka Discovery Client

### Funcionalidades a implementar

1. **Consumer:** escuchar `OrderCreatedEvent` y `PaymentProcessedEvent`
2. **Notificaciones:** log en consola simulando envío de email
3. **Templates:** mensajes personalizados por tipo de evento

### Conceptos a aprender en esta fase

- Múltiples consumers en el mismo grupo
- Procesamiento de distintos tipos de eventos
- Preparación para integración con JavaMail o SendGrid

---

## 9. Fase 6 — API Gateway

### Objetivo

Punto de entrada único para todos los servicios. Valida JWT y enruta peticiones.

### Dependencias (Spring Initializr)

- Spring Cloud Gateway
- Eureka Discovery Client
- Lombok

### Funcionalidades a implementar

1. **Rutas:** configurar rutas hacia cada microservicio
2. **Filtro JWT:** validar token en cada request entrante
3. **Rate limiting:** limitar peticiones por usuario
4. **CORS:** configuración para frontend

### Configuración de rutas (application.yml)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
```

### Conceptos a aprender en esta fase

- Spring Cloud Gateway y sus filtros
- Load balancing con `lb://` prefix
- Global filters vs Route filters
- Propagación de headers entre servicios

---

## 10. Fase 7 — Service Discovery con Eureka

### Objetivo

Registro automático de microservicios para que puedan encontrarse sin IPs hardcodeadas.

### Dependencias (Spring Initializr)

- Eureka Server

### Configuración básica

```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

### Conceptos a aprender en esta fase

- Service Registry pattern
- Self-registration de servicios
- Health checks automáticos
- Dashboard de Eureka en http://localhost:8761

---

## 11. Fase 8 — Comunicación Asíncrona con Kafka

### Objetivo

Configurar Kafka local con Docker y verificar la comunicación entre servicios.

### Levantar Kafka con Docker Compose

```yaml
version: "3"
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    depends_on:
      - zookeeper
```

### Topics a crear

| Topic             | Productor       | Consumidores                          |
| ----------------- | --------------- | ------------------------------------- |
| order-created     | order-service   | payment-service, notification-service |
| payment-processed | payment-service | order-service, notification-service   |

### Conceptos a aprender en esta fase

- Topics, particiones y offsets
- Consumer groups y paralelismo
- Serialización con JSON
- Kafka UI para monitoreo visual

---

## 12. Fase 9 — Contenerización con Docker

### Objetivo

Crear imágenes Docker para cada microservicio y levantar todo con Docker Compose.

### Dockerfile base (para cada servicio)

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose completo

```yaml
version: "3.8"
services:
  postgres-users:
    image: postgres:15
    environment:
      POSTGRES_DB: users_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  eureka-server:
    build: ./eureka-server
    ports:
      - "8761:8761"

  user-service:
    build: ./user-service
    ports:
      - "8081:8081"
    depends_on:
      - postgres-users
      - eureka-server

  # ... resto de servicios
```

### Conceptos a aprender en esta fase

- Multi-stage builds para optimizar imágenes
- Docker networking entre contenedores
- Variables de entorno con `.env`
- Health checks en Docker Compose
- `depends_on` y orden de arranque

---

## 13. Fase 10 — Orquestación con Kubernetes

### Objetivo

Desplegar todos los microservicios en un clúster local con Minikube.

### Estructura de manifiestos

```
k8s/
├── configmaps/
│   └── app-config.yaml
├── deployments/
│   ├── user-service-deployment.yaml
│   ├── product-service-deployment.yaml
│   └── ...
├── services/
│   ├── user-service-svc.yaml
│   └── ...
└── ingress/
    └── api-gateway-ingress.yaml
```

### Deployment base (ejemplo user-service)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
        - name: user-service
          image: user-service:latest
          ports:
            - containerPort: 8081
          env:
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                configMapKeyRef:
                  name: app-config
                  key: users-db-url
```

### Conceptos a aprender en esta fase

- Deployments, Services e Ingress
- ConfigMaps y Secrets
- Horizontal Pod Autoscaler (HPA)
- Rolling updates y rollbacks
- kubectl comandos básicos
- Minikube para desarrollo local

---

## 15. Checklist general de desarrollo

Esta guía también puede usarse como lista de verificación general:

- [ ] Instalar JDK 21 y Maven
- [ ] Inicializar cada microservicio desde Spring Initializr
- [ ] Configurar `application.yml`/`application.properties` de cada servicio
- [ ] Crear roles y bases de datos PostgreSQL necesarias
- [ ] Levantar Kafka si se requiere mensajería asíncrona
- [ ] Probar cada microservicio de forma independiente
- [ ] Registrar servicios en Eureka (fase de discovery)
- [ ] Agregar Gateway y pruebas de rutas finales
- [ ] Contenerizar cada servicio con Docker
- [ ] Desplegar con Kubernetes o Docker Compose

## 16. Tecnologías y Versiones

| Tecnología      | Versión | Uso                          |
| --------------- | ------- | ---------------------------- |
| Java            | 21 LTS  | Lenguaje base                |
| Spring Boot     | 3.4.x   | Framework de microservicios  |
| Spring Cloud    | 2024.x  | Gateway, Eureka, Feign       |
| Spring Security | 6.x     | Autenticación y autorización |
| Spring Data JPA | 3.x     | Persistencia de datos        |
| Apache Kafka    | 3.7.x   | Mensajería asíncrona         |
| PostgreSQL      | 15      | Base de datos relacional     |
| Docker          | 25.x    | Contenerización              |
| Kubernetes      | 1.29.x  | Orquestación                 |
| JWT (jjwt)      | 0.12.x  | Tokens de autenticación      |
| Lombok          | 1.18.x  | Reducción de boilerplate     |
| MapStruct       | 1.5.x   | Mapeo entre entidades y DTOs |
| Maven           | 3.9.x   | Build tool                   |

---

## Notas de desarrollo

- Construir y probar cada servicio de forma independiente antes de integrarlo
- Usar Postman o Thunder Client para probar los endpoints en cada fase
- Mantener un archivo `README.md` por servicio con instrucciones de ejecución
- Hacer commits frecuentes en Git con mensajes descriptivos por feature
- Revisar los logs de Kafka y Eureka ante cualquier problema de comunicación
