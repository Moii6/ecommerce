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
14. [Fase 11 — Testing con JUnit 5, Mockito y Testcontainers](#14-fase-11--testing-con-junit-5-mockito-y-testcontainers)
15. [Fase 12 — Observabilidad con Actuator, Prometheus y Grafana](#15-fase-12--observabilidad-con-actuator-prometheus-y-grafana)
16. [Fase 13 — Documentación con Swagger / OpenAPI 3](#16-fase-13--documentación-con-swagger--openapi-3)
17. [Patrones de Diseño aplicados en el proyecto](#17-patrones-de-diseño-aplicados-en-el-proyecto)
18. [Checklist general de desarrollo](#18-checklist-general-de-desarrollo)
19. [Tecnologías y Versiones](#19-tecnologías-y-versiones)

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

## 4.1 Patrón de diseño: Strategy Pattern en user-service

### ¿Qué es el Strategy Pattern?

El **Strategy Pattern** (patrón Estrategia) es un patrón de comportamiento que define una **familia de algoritmos intercambiables**, los encapsula en clases separadas y los hace sustituibles sin modificar el código que los usa.

**Regla clave:** el código que ejecuta la lógica no necesita saber *cuál* estrategia se usa, solo sabe que todas cumplen la misma interfaz.

### ¿Por qué aplicarlo aquí?

Cuando un usuario se registra, hay múltiples reglas de validación: formato de email, longitud de contraseña, nombre no vacío, etc. Sin el patrón, todas esas reglas quedan mezcladas dentro de `UserService`. Con Strategy, cada regla es una clase independiente — si mañana quieres agregar o quitar una validación, tocas solo esa clase.

### Diagrama del patrón

```
           «interface»
        ValidationStrategy
        +validate(User): void
               ▲
       ┌───────┴────────┐
       │                │
EmailValidation   PasswordValidation
Strategy          Strategy
+validate(User)   +validate(User)
       │                │
       └───────┬────────┘
               │  uses List<ValidationStrategy>
        UserServiceImpl
        +register(User): User
```

### Paso 1 — Agregar el campo `password` a la entidad `User`

La guía (Fase 1) requiere que `User` tenga `password`. Agrégalo en `User.java`:

```java
@Column(nullable = false)
private String password;

// getter y setter
public String getPassword() { return password; }
public void setPassword(String password) { this.password = password; }
```

### Paso 2 — Crear la interfaz `ValidationStrategy`

Crea el paquete `strategy` dentro de `com.test.demo` y agrega la interfaz:

```
src/main/java/com/test/demo/strategy/ValidationStrategy.java
```

```java
package com.test.demo.strategy;

import com.test.demo.User;

public interface ValidationStrategy {
    void validate(User user);
}
```

Esta interfaz es el **contrato** que todas las estrategias deben cumplir.

### Paso 3 — Crear `EmailValidationStrategy`

```
src/main/java/com/test/demo/strategy/EmailValidationStrategy.java
```

```java
package com.test.demo.strategy;

import com.test.demo.User;
import org.springframework.stereotype.Component;

@Component
public class EmailValidationStrategy implements ValidationStrategy {

    private static final String EMAIL_REGEX = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";

    @Override
    public void validate(User user) {
        String email = user.getEmail();
        if (email == null || !email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Email inválido: " + email);
        }
    }
}
```

### Paso 4 — Crear `PasswordValidationStrategy`

```
src/main/java/com/test/demo/strategy/PasswordValidationStrategy.java
```

```java
package com.test.demo.strategy;

import com.test.demo.User;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidationStrategy implements ValidationStrategy {

    private static final int MIN_LENGTH = 8;

    @Override
    public void validate(User user) {
        String password = user.getPassword();
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                "La contraseña debe tener al menos " + MIN_LENGTH + " caracteres"
            );
        }
    }
}
```

### Paso 5 — Crear la interfaz `UserService`

Crea el paquete `service` y la interfaz:

```
src/main/java/com/test/demo/service/UserService.java
```

```java
package com.test.demo.service;

import com.test.demo.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User register(User user);
    Optional<User> findById(Long id);
    User findByEmail(String email);
    List<User> findAll();
    void delete(Long id);
}
```

### Paso 6 — Crear `UserServiceImpl` (usa las estrategias)

```
src/main/java/com/test/demo/service/UserServiceImpl.java
```

```java
package com.test.demo.service;

import com.test.demo.User;
import com.test.demo.UserRepository;
import com.test.demo.strategy.ValidationStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final List<ValidationStrategy> validationStrategies;

    // Spring inyecta automáticamente todos los beans que implementan ValidationStrategy
    public UserServiceImpl(UserRepository userRepository,
                           List<ValidationStrategy> validationStrategies) {
        this.userRepository = userRepository;
        this.validationStrategies = validationStrategies;
    }

    @Override
    public User register(User user) {
        // Aplica TODAS las estrategias antes de guardar
        validationStrategies.forEach(strategy -> strategy.validate(user));
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
```

> **Clave del patrón:** `UserServiceImpl` no conoce `EmailValidationStrategy` ni `PasswordValidationStrategy` por nombre. Solo sabe que recibe una lista de `ValidationStrategy`. Si agregas una tercera estrategia con `@Component`, Spring la incluye automáticamente — sin tocar `UserServiceImpl`.

### Paso 7 — Actualizar el controlador para usar `UserService`

Reemplaza la inyección directa de `UserRepository` en `HelloController` por `UserService`:

```java
@RestController
@RequestMapping("/api/users")
public class HelloController {

    @Autowired
    private UserService userService;  // ya no accede directo al repositorio

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return userService.register(user);
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id).orElse(null);
    }

    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userService.findAll();
    }

    @GetMapping("/email/{email}")
    public User getUserByEmail(@PathVariable String email) {
        return userService.findByEmail(email);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

### Paso 8 — Probar el patrón con curl

**Registro válido:**
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Juan","email":"juan@mail.com","password":"segura123"}'
```

**Email inválido — debe lanzar error:**
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Juan","email":"no-es-email","password":"segura123"}'
```

**Contraseña corta — debe lanzar error:**
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Juan","email":"juan@mail.com","password":"123"}'
```

### Paso 9 — Agregar una nueva estrategia (extensibilidad del patrón)

Para agregar la validación de que el nombre no puede estar vacío, solo creas una clase nueva — sin modificar nada existente:

```java
package com.test.demo.strategy;

import com.test.demo.User;
import org.springframework.stereotype.Component;

@Component
public class NameValidationStrategy implements ValidationStrategy {

    @Override
    public void validate(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
    }
}
```

Spring detecta el `@Component` y la agrega automáticamente a la lista en `UserServiceImpl`. Este es el **principio Open/Closed** en acción: abierto para extensión, cerrado para modificación.

### Estructura de carpetas resultante

```
com.test.demo/
├── strategy/
│   ├── ValidationStrategy.java        ← interfaz (el "contrato")
│   ├── EmailValidationStrategy.java   ← estrategia 1
│   ├── PasswordValidationStrategy.java← estrategia 2
│   └── NameValidationStrategy.java    ← estrategia 3 (opcional)
├── service/
│   ├── UserService.java               ← interfaz del servicio
│   └── UserServiceImpl.java           ← usa las estrategias
├── User.java
├── UserRepository.java
└── HelloController.java               ← usa UserService, no el repo directo
```

### Checklist de Strategy Pattern

- [ ] Agregar campo `password` a la entidad `User`
- [ ] Crear interfaz `ValidationStrategy` en paquete `strategy`
- [ ] Crear `EmailValidationStrategy` con `@Component`
- [ ] Crear `PasswordValidationStrategy` con `@Component`
- [ ] Crear interfaz `UserService` en paquete `service`
- [ ] Crear `UserServiceImpl` que recibe `List<ValidationStrategy>`
- [ ] Actualizar `HelloController` para inyectar `UserService`
- [ ] Probar registro válido e inválido con curl o Postman
- [ ] Agregar una tercera estrategia y verificar que funciona sin tocar `UserServiceImpl`

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

## 14. Fase 11 — Testing con JUnit 5, Mockito y Testcontainers

### Objetivo

Agregar pruebas unitarias e integración a cada microservicio. Es uno de los skills más evaluados en entrevistas técnicas senior.

### Dependencias (agregar a cada servicio)

```xml
<!-- JUnit 5 + Mockito — ya incluido en spring-boot-starter-test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers — para pruebas de integración con BD real -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

---

### Tipo 1 — Unit Test con Mockito (user-service)

Prueba el servicio en aislamiento — sin base de datos, sin Spring context.

```java
// src/test/java/com/example/userservice/service/UserServiceTest.java

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_ShouldSaveUser_WhenValidData() {
        // Arrange
        User user = new User();
        user.setEmail("juan@mail.com");
        user.setPassword("segura123");
        user.setName("Juan");

        when(passwordEncoder.encode("segura123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        User result = userService.register(user);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(user);
        verify(passwordEncoder, times(1)).encode("segura123");
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        // Arrange
        User user = new User();
        user.setEmail("juan@mail.com");
        when(userRepository.existsByEmail("juan@mail.com")).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.register(user));
        verify(userRepository, never()).save(any());
    }
}
```

**Conceptos clave:**
- `@ExtendWith(MockitoExtension.class)` — activa Mockito sin levantar Spring
- `@Mock` — crea un mock del repositorio
- `@InjectMocks` — inyecta los mocks en el servicio
- `when(...).thenReturn(...)` — define comportamiento del mock
- `verify(...)` — verifica que se llamó el método esperado

---

### Tipo 2 — Integration Test con Testcontainers (user-service)

Prueba con una base de datos PostgreSQL real corriendo en Docker.

```java
// src/test/java/com/example/userservice/UserServiceIntegrationTest.java

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("users_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_ShouldReturn201_WhenValidUser() throws Exception {
        Map<String, String> request = Map.of(
            "name", "Juan",
            "email", "juan@mail.com",
            "password", "segura123"
        );

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("juan@mail.com"));
    }

    @Test
    void login_ShouldReturnJwt_WhenValidCredentials() throws Exception {
        // Primero registrar
        // ... (similar al test anterior)

        // Luego login
        Map<String, String> loginRequest = Map.of(
            "email", "juan@mail.com",
            "password", "segura123"
        );

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }
}
```

**Conceptos clave:**
- `@Testcontainers` — levanta contenedores Docker antes del test
- `@Container` — define el contenedor PostgreSQL
- `@DynamicPropertySource` — inyecta la URL de la BD del contenedor al contexto de Spring
- `MockMvc` — simula peticiones HTTP sin levantar servidor real

---

### Tipo 3 — Test de Kafka Consumer (order-service)

```java
// src/test/java/com/example/orderservice/kafka/OrderEventConsumerTest.java

@SpringBootTest
@Testcontainers
class OrderEventConsumerTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void shouldProcessOrderCreatedEvent() throws InterruptedException {
        OrderCreatedEvent event = new OrderCreatedEvent("order-123", "user-456", 150.00);
        kafkaTemplate.send("order-created", event);

        // Esperar procesamiento asíncrono
        Thread.sleep(2000);

        // Verificar que se procesó
        // (usar un spy o verificar en BD que el estado cambió)
    }
}
```

---

### Estructura de tests recomendada por servicio

```
src/test/java/com/example/userservice/
├── service/
│   └── UserServiceTest.java          ← unit tests (Mockito)
├── controller/
│   └── UserControllerTest.java       ← tests de endpoints (MockMvc)
├── repository/
│   └── UserRepositoryTest.java       ← tests de queries JPA
└── UserServiceIntegrationTest.java   ← integration test (Testcontainers)
```

### Comandos para ejecutar tests

```bash
# Solo unit tests
./mvnw test

# Solo integration tests
./mvnw verify -P integration-tests

# Con reporte de cobertura (JaCoCo)
./mvnw test jacoco:report
```

### Checklist de Testing

- [ ] Unit test para cada método de servicio (happy path + error path)
- [ ] Integration test con Testcontainers para al menos user-service y order-service
- [ ] Test de Kafka consumer en payment-service
- [ ] Test de endpoints REST con MockMvc
- [ ] Verificar que todos los tests pasan con `./mvnw test`

---

## 15. Fase 12 — Observabilidad con Actuator, Prometheus y Grafana

### Objetivo

Monitorear el estado y métricas de cada microservicio en tiempo real. Habilidad clave para roles senior en entrevistas.

### Dependencias (agregar a cada servicio)

```xml
<!-- Spring Actuator — expone métricas y health checks -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer — exporta métricas a Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Configuración application.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
```

### Endpoints expuestos por Actuator

| Endpoint | URL | Descripción |
|---|---|---|
| Health check | /actuator/health | Estado del servicio y dependencias |
| Info | /actuator/info | Información del build |
| Métricas | /actuator/metrics | Lista de métricas disponibles |
| Prometheus | /actuator/prometheus | Métricas en formato Prometheus |

### Levantar Prometheus + Grafana con Docker Compose

```yaml
# Agregar al docker-compose.yml principal

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    depends_on:
      - prometheus
```

### prometheus.yml (en la raíz del proyecto)

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['user-service:8081']

  - job_name: 'order-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['order-service:8083']

  - job_name: 'payment-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['payment-service:8084']
```

### Métricas clave a monitorear en Grafana

| Métrica | Descripción |
|---|---|
| `http_server_requests_seconds` | Latencia de endpoints REST |
| `jvm_memory_used_bytes` | Uso de memoria JVM |
| `kafka_consumer_fetch_rate` | Velocidad de consumo Kafka |
| `hikaricp_connections_active` | Pool de conexiones BD |
| `process_cpu_usage` | Uso de CPU por servicio |

### Trazabilidad distribuida con Micrometer Tracing

```xml
<!-- Para correlacionar requests entre microservicios -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% en desarrollo
```

Esto agrega un `traceId` a cada request que se propaga entre servicios — fundamental para diagnosticar fallas en sistemas distribuidos.

### Checklist de Observabilidad

- [ ] Agregar `spring-boot-starter-actuator` a todos los servicios
- [ ] Configurar exposición de endpoint `/actuator/prometheus`
- [ ] Agregar Prometheus y Grafana al docker-compose.yml
- [ ] Crear prometheus.yml con scrape config de cada servicio
- [ ] Verificar métricas en http://localhost:9090 (Prometheus)
- [ ] Crear dashboard básico en http://localhost:3000 (Grafana)
- [ ] Configurar Micrometer Tracing para correlación de requests

---

## 16. Fase 13 — Documentación con Swagger / OpenAPI 3

### Objetivo

Documentar todos los endpoints REST automáticamente. Requerido en prácticamente todas las vacantes enterprise.

### Dependencias

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### Configuración básica (application.yml)

```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
```

### Anotaciones en el controlador

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Gestión de usuarios y autenticación")
public class UserController {

    @PostMapping("/register")
    @Operation(
        summary = "Registrar usuario",
        description = "Crea un nuevo usuario en el sistema"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "409", description = "Email ya registrado")
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        // ...
    }
}
```

### URLs de documentación por servicio

| Servicio | Swagger UI |
|---|---|
| user-service | http://localhost:8081/swagger-ui.html |
| product-service | http://localhost:8082/swagger-ui.html |
| order-service | http://localhost:8083/swagger-ui.html |

### Checklist de Documentación

- [ ] Agregar springdoc-openapi a todos los servicios
- [ ] Anotar controladores con `@Tag` y `@Operation`
- [ ] Definir `@ApiResponse` para cada endpoint
- [ ] Verificar Swagger UI en cada servicio
- [ ] Documentar el modelo de request y response con `@Schema`

---

## 17. Patrones de Diseño aplicados en el proyecto

Esta sección consolida los patrones que ya están implementados en la guía y los que puedes mencionar en entrevista técnica.

### Patrones implementados

| Patrón | Dónde se aplica | Para qué sirve |
|---|---|---|
| **Strategy** | user-service — validaciones | Algoritmos intercambiables sin modificar el cliente |
| **Repository** | Todos los servicios — JPA | Abstrae el acceso a datos |
| **DTO** | Todos los servicios | Separa la capa de dominio de la API |
| **Factory** | Spring IoC Container | Creación de objetos gestionada por el framework |
| **Observer** | Kafka eventos | Notificación asíncrona entre servicios |
| **Gateway** | api-gateway | Punto de entrada único al sistema |
| **Saga** | order-service + payment-service | Transacciones distribuidas con compensación |
| **Circuit Breaker** | Resilience4j en OpenFeign | Resiliencia ante fallos de servicios |

### Principios SOLID aplicados

| Principio | Dónde se ve en el proyecto |
|---|---|
| **S** — Single Responsibility | Cada servicio tiene una sola responsabilidad (users, products, orders...) |
| **O** — Open/Closed | Strategy Pattern — agregar validaciones sin modificar UserServiceImpl |
| **L** — Liskov Substitution | UserService interfaz — cualquier impl es intercambiable |
| **I** — Interface Segregation | Interfaces específicas por servicio en lugar de una genérica |
| **D** — Dependency Inversion | Inyección de dependencias con Spring — las clases dependen de abstracciones |

### Cómo hablar de esto en entrevista

> *"In this project I applied several design patterns. For example, I used the Strategy Pattern in the user service to encapsulate validation rules — each rule is an independent class that implements a common interface. This follows the Open/Closed principle: I can add new validations without modifying the existing service. I also applied the Observer pattern through Kafka — when an order is created, payment and notification services react independently to that event without the order service knowing about them."*

---

## 18. Checklist general de desarrollo

- [ ] Instalar JDK 21 y Maven
- [ ] Inicializar cada microservicio desde Spring Initializr
- [ ] Configurar `application.yml` de cada servicio
- [ ] Crear bases de datos PostgreSQL por servicio
- [ ] **Fase 1:** user-service con JWT y Spring Security
- [ ] **Fase 2:** product-service con CRUD y paginación
- [ ] **Fase 3:** order-service con Kafka producer y Feign
- [ ] **Fase 4:** payment-service con Kafka consumer y DLQ
- [ ] **Fase 5:** notification-service con múltiples consumers
- [ ] **Fase 6:** API Gateway con enrutamiento y filtro JWT
- [ ] **Fase 7:** Eureka Server para service discovery
- [ ] **Fase 8:** Kafka local con Docker Compose
- [ ] **Fase 9:** Dockerizar cada servicio
- [ ] **Fase 10:** Desplegar en Kubernetes con Minikube
- [ ] **Fase 11:** Unit tests + Integration tests con Testcontainers
- [ ] **Fase 12:** Actuator + Prometheus + Grafana
- [ ] **Fase 13:** Swagger/OpenAPI en todos los servicios
- [ ] Documentar cada servicio con README.md
- [ ] Subir a GitHub con commits descriptivos por feature

---

## 19. Tecnologías y Versiones

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 LTS | Lenguaje base |
| Spring Boot | 3.4.x | Framework de microservicios |
| Spring Cloud | 2024.x | Gateway, Eureka, Feign |
| Spring Security | 6.x | Autenticación y autorización |
| Spring Data JPA | 3.x | Persistencia de datos |
| Apache Kafka | 3.7.x | Mensajería asíncrona |
| PostgreSQL | 15 | Base de datos relacional |
| Docker | 25.x | Contenerización |
| Kubernetes | 1.29.x | Orquestación |
| JWT (jjwt) | 0.12.x | Tokens de autenticación |
| Lombok | 1.18.x | Reducción de boilerplate |
| MapStruct | 1.5.x | Mapeo entre entidades y DTOs |
| Maven | 3.9.x | Build tool |
| JUnit 5 | 5.10.x | Unit testing |
| Mockito | 5.x | Mocking en tests |
| Testcontainers | 1.19.x | Integration testing con Docker |
| Spring Actuator | 3.4.x | Health checks y métricas |
| Micrometer | 1.12.x | Exportación de métricas |
| Prometheus | 2.x | Recolección de métricas |
| Grafana | 10.x | Dashboards de monitoreo |
| Micrometer Tracing | 1.2.x | Trazabilidad distribuida |
| Zipkin | 3.x | Visualización de trazas |
| Springdoc OpenAPI | 2.3.x | Documentación Swagger |
| Resilience4j | 2.x | Circuit breaker y retry |

---

## Notas de desarrollo

- Construir y probar cada servicio de forma independiente antes de integrarlo
- Usar Postman o Thunder Client para probar los endpoints en cada fase
- Mantener un archivo `README.md` por servicio con instrucciones de ejecución
- Hacer commits frecuentes en Git con mensajes descriptivos por feature
- Revisar los logs de Kafka y Eureka ante cualquier problema de comunicación
- Ejecutar `./mvnw test` antes de cada commit para asegurar que no se rompen tests
- Revisar métricas en Grafana después de cada integración para detectar degradación de rendimiento

