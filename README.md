# ByteBites: A Secure Microservices Platform

ByteBites is a cloud-native microservices system designed for an online food delivery startup. This platform connects customers with local restaurants, providing a scalable, secure, and resilient architecture built using Spring Boot, Spring Cloud, JWT, OAuth2, and event-driven communication via RabbitMQ.

## Table of Contents

1.  [Project Overview](#1-project-overview)
2.  [Architecture](#2-architecture)
3.  [Services Implemented](#3-services-implemented)
4.  [Key Features & Technologies](#4-key-features--technologies)
5.  [Setup and Running Locally](#5-setup-and-running-locally)
6.  [Sample API Endpoints](#6-sample-api-endpoints)
7.  [Future Enhancements](#7-future-enhancements)
8.  [Contributing](#8-contributing)
9.  [License](#9-license)

---

## 1. Project Overview

The ByteBites platform aims to facilitate food ordering by connecting customers with restaurants. It's designed for rapid growth, emphasizing modularity, security, and resilience through a microservices approach.

## 2. Architecture

The architecture follows a standard microservices pattern, leveraging Spring Cloud components for service discovery, centralized configuration, and API Gateway. Security is handled by a dedicated authentication service using JWTs, and inter-service communication is facilitated by both direct REST calls (protected by Circuit Breakers) and asynchronous event messaging via RabbitMQ.

**Core Components:**

* **Service Discovery (Eureka Server):** For services to register and discover each other.
* **Centralized Configuration (Config Server):** Manages externalized configuration for all microservices.
* **API Gateway:** Routes external requests to the appropriate microservices, handles cross-cutting concerns like authentication, authorization (initial JWT validation), and logging.
* **Authentication Service (Auth Service):** Handles user registration, login, JWT token generation, and user role management.
* **Restaurant Service:** Manages restaurant information, menus, and menu items.
* **Order Service:** Handles customer order placement, tracking, and management.
* **Notification Service (Planned):** Will handle sending notifications (e.g., email, SMS) based on events.
* **RabbitMQ:** Message broker for asynchronous event communication.
* **H2 Database:** In-memory database for local development (can be switched to PostgreSQL/MySQL for production).

## 3. Services Implemented

As of now, the following services are implemented:

* **Eureka Server:** Service Discovery.
* **Config Server:** Centralized Configuration Management.
* **Auth Service:** User Authentication and Authorization (JWT generation, RBAC).
* **API Gateway:** Request Routing, Security Filters (JWT validation, header forwarding).
* **Restaurant Service:** Manages restaurants and their menu items.
* **Order Service:**
    * Creates new orders with line items.
    * Calculates total order amount.
    * Integrates with `restaurant-service` via Feign Client to fetch real menu item details and prices during order creation.
    * Employs Resilience4j Circuit Breaker for fault tolerance when calling `restaurant-service`.
    * Publishes `OrderPlacedEvent` to RabbitMQ.
    * Allows viewing orders by customer (resource ownership) and by restaurant (placeholder ownership check).

## 4. Key Features & Technologies

* **Microservices Architecture:** Modular and scalable design.
* **Spring Boot & Spring Cloud:** Core framework for building microservices.
* **Service Discovery:** Eureka Server.
* **Centralized Configuration:** Spring Cloud Config.
* **API Gateway:** Spring Cloud Gateway.
* **Security:**
    * JWT-based security managed by `auth-service`.
    * OAuth2 integration (e.g., Google/GitHub login - *planned for auth-service*).
    * Role-Based Access Control (RBAC): `ROLE_CUSTOMER`, `ROLE_RESTAURANT_OWNER`, `ROLE_ADMIN`.
    * Stateless Sessions: Token-based security.
    * Resource Ownership Checks: Users can access only their data (e.g., customers view their own orders).
    * Inter-service communication secured via headers (`X-User-ID`, `X-User-Roles`, etc.) forwarded by the API Gateway.
* **Event-Driven Communication:** RabbitMQ for decoupling services (`OrderPlacedEvent`).
* **Resilience:** Resilience4j for Circuit Breaker patterns (e.g., protecting `order-service` calls to `restaurant-service`).
* **Data Persistence:** Spring Data JPA with H2 (in-memory) for development.
* **Lombok:** Reduces boilerplate code.

## 5. Setup and Running Locally

To set up and run the ByteBites platform locally, follow these steps:

### Prerequisites

* Java 17+ JDK installed
* Maven 3.x installed
* Docker (Optional, for running RabbitMQ easily)
* Git

### Steps

1.  **Clone the Monorepo:**
    ```bash
    git clone https://github.com/baaki20/ByteBites-Restaurant.git
    cd bytebites-platform # Or whatever your monorepo root folder is named
    ```

2.  **Start RabbitMQ:**
    If you have Docker, the easiest way to run RabbitMQ:
    ```bash
    docker run -d --hostname my-rabbit --name some-rabbit -p 15672:15672 -p 5672:5672 rabbitmq:3-management
    ```
    (Access management UI at `http://localhost:15672` with `guest`/`guest`)
    Otherwise, install and start RabbitMQ manually.

3.  **Start Eureka Server:**
    Navigate to the `eureka-server` directory and run:
    ```bash
    cd eureka-server
    mvn spring-boot:run
    ```
    (Runs on `http://localhost:8761`)

4.  **Start Config Server:**
    Ensure your `config-repo` is set up and accessible.
    ```bash
    cd config-server
    mvn spring-boot:run
    ```
    (Runs on `http://localhost:8888`)

5.  **Start Auth Service:**
    ```bash
    cd auth-service
    mvn spring-boot:run
    ```
    (Runs on `http://localhost:8080`)

6.  **Start Restaurant Service:**
    **Note:** Ensure you have implemented the `GET /api/restaurants/menu-items/{menuItemId}` endpoint in your `restaurant-service` as discussed.
    ```bash
    cd restaurant-service
    mvn spring-boot:run
    ```
    (Runs on `http://localhost:8084`)

7.  **Start Order Service:**
    ```bash
    cd order-service
    mvn spring-boot:run
    ```
    (Runs on `http://localhost:8086`)

8.  **Start API Gateway:**
    ```bash
    cd api-gateway
    mvn spring-boot:run
    ```
    (Runs on `http://localhost:8082`)

    **Important API Gateway Configuration:**
    Ensure your API Gateway is configured to:
    * Route requests to the correct services.
    * Validate JWTs from the `auth-service`.
    * Forward user details (e.g., `user_id`, `roles`) extracted from the JWT as custom headers (e.g., `X-User-ID`, `X-User-Roles`, `X-Customer-ID`) to downstream microservices like `restaurant-service` and `order-service`.

## 6. Sample API Endpoints

Here are some of the key API endpoints and their required roles:

| Endpoint                       | Method | Role Required                                       | Description                                       |
| :----------------------------- | :----- | :-------------------------------------------------- | :------------------------------------------------ |
| `/auth/register`               | POST   | Public                                              | Register a new user.                              |
| `/auth/login`                  | POST   | Public                                              | Authenticate and get JWT token.                   |
| `/api/restaurants`             | GET    | Authenticated                                       | Get a list of all restaurants.                    |
| `/api/restaurants`             | POST   | `ROLE_RESTAURANT_OWNER`                             | Create a new restaurant.                          |
| `/api/restaurants/menu-items/{menuItemId}` | GET    | Authenticated (internal call from Order Service)    | Get details of a specific menu item.              |
| `/api/orders`                  | POST   | `ROLE_CUSTOMER`                                     | Place a new food order.                           |
| `/api/orders/{id}`             | GET    | Resource owner (`ROLE_CUSTOMER`), `ROLE_RESTAURANT_OWNER` (for their restaurants), `ROLE_ADMIN` | Get details of a specific order.                  |
| `/api/orders/my-orders`        | GET    | `ROLE_CUSTOMER`                                     | Get all orders placed by the authenticated customer. |
| `/api/orders/restaurant/{restaurantId}` | GET    | `ROLE_RESTAURANT_OWNER`, `ROLE_ADMIN`              | Get all orders for a specific restaurant.         |
| `/admin/users`                 | GET    | `ROLE_ADMIN` only                                   | View all users (planned for admin service).       |

---

## 7. Future Enhancements

* **Notification Service:** Implement the `notification-service` to consume `OrderPlacedEvent` and send notifications (email/SMS).
* **Payment Service:** Integrate a payment gateway.
* **Delivery Service:** Manage delivery logistics.
* **Robust Restaurant Ownership Check:** Enhance `order-service` to call `restaurant-service` for explicit verification of restaurant ownership for `ROLE_RESTAURANT_OWNER` viewing orders.
* **Global Exception Handling:** Implement `@ControllerAdvice` for consistent error responses.
* **Distributed Tracing:** Integrate Zipkin/Sleuth for monitoring request flow across microservices.
* **Centralized Logging:** Use ELK stack (Elasticsearch, Logstash, Kibana) or Grafana Loki.
* **Monitoring & Metrics:** Integrate Prometheus and Grafana.
* **Containerization:** Dockerize all services and orchestrate with Docker Compose or Kubernetes.
* **External Database:** Transition from H2 to PostgreSQL or MySQL for persistent data.
* **Security Hardening:** Implement more advanced security measures (e.g., input validation, rate limiting at gateway, fine-grained authorization).

## 8. Contributing

Feel free to fork the repository and contribute! Please open an issue first to discuss major changes.