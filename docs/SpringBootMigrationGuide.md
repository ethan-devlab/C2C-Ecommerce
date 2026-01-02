# C2C E-Commerce Platform: Spring Boot GUI Migration Guide

This document provides a comprehensive guide for converting the existing CLI-based C2C E-Commerce Platform into a web-based GUI application using **Spring Boot**.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Current Architecture Analysis](#2-current-architecture-analysis)
3. [Target Architecture](#3-target-architecture)
4. [Dependencies Setup](#4-dependencies-setup)
5. [Project Structure Changes](#5-project-structure-changes)
6. [Spring Boot Configuration](#6-spring-boot-configuration)
7. [Repository Layer Migration](#7-repository-layer-migration)
8. [Service Layer Migration](#8-service-layer-migration)
9. [REST Controller Implementation](#9-rest-controller-implementation)
10. [Frontend Options](#10-frontend-options)
11. [Security Implementation](#11-security-implementation)
12. [Migration Checklist](#12-migration-checklist)
13. [Sample Code Examples](#13-sample-code-examples)

---

## 1. Overview

### Current State
- **CLI Application**: Command-line interface in `Main.java`
- **Java 23**: Modern Java with pattern matching
- **No Framework**: Manual dependency injection
- **In-Memory Storage**: Custom `InMemory*Repository` implementations
- **Layered Architecture**: Repository → Service → CLI

### Target State
- **Web Application**: REST API + Web UI
- **Spring Boot 3.x**: Enterprise-grade framework
- **Spring Data JPA**: Database abstraction (optional H2/PostgreSQL)
- **Thymeleaf/React**: Frontend rendering
- **Spring Security**: Authentication & authorization

---

## 2. Current Architecture Analysis

### Service Layer Interfaces (to be preserved)

| Service | Interface | Key Methods |
|---------|-----------|-------------|
| **UserService** | `c2c.user.UserService` | `register()`, `login()`, `updateProfile()` |
| **ProductService** | `c2c.product.ProductService` | `create()`, `update()`, `list()`, `get()` |
| **CartService** | `c2c.cart.CartService` | `addItem()`, `updateQuantity()`, `removeItem()`, `view()` |
| **OrderService** | `c2c.order.OrderService` | `createFromCart()`, `get()`, `listByBuyer()`, `updateStatus()` |
| **PaymentService** | `c2c.payment.PaymentService` | `pay()` |
| **ReviewService** | `c2c.review.ReviewService` | `addReview()`, `list()`, `averageRating()` |

### CLI Commands to REST Endpoints Mapping

| CLI Command | HTTP Method | REST Endpoint | Controller |
|-------------|-------------|---------------|------------|
| `register` | POST | `/api/users/register` | UserController |
| `login` | POST | `/api/auth/login` | AuthController |
| `users` | GET | `/api/users` | UserController |
| `user <id>` | GET | `/api/users/{id}` | UserController |
| `update-profile` | PUT | `/api/users/{id}` | UserController |
| `add-product` | POST | `/api/products` | ProductController |
| `update-product` | PUT | `/api/products/{id}` | ProductController |
| `products` | GET | `/api/products` | ProductController |
| `product <id>` | GET | `/api/products/{id}` | ProductController |
| `search` | GET | `/api/products/search?q=` | ProductController |
| `cart <userId>` | GET | `/api/cart/{userId}` | CartController |
| `cart-add` | POST | `/api/cart/{userId}/items` | CartController |
| `cart-update` | PUT | `/api/cart/{userId}/items/{productId}` | CartController |
| `cart-remove` | DELETE | `/api/cart/{userId}/items/{productId}` | CartController |
| `checkout` | POST | `/api/orders` | OrderController |
| `orders` | GET | `/api/orders` | OrderController |
| `order <id>` | GET | `/api/orders/{id}` | OrderController |
| `order-status` | PATCH | `/api/orders/{id}/status` | OrderController |
| `pay` | POST | `/api/payments` | PaymentController |
| `payments` | GET | `/api/payments` | PaymentController |
| `review` | POST | `/api/reviews` | ReviewController |
| `reviews <productId>` | GET | `/api/products/{id}/reviews` | ReviewController |
| `rating <productId>` | GET | `/api/products/{id}/rating` | ReviewController |
| `stats` | GET | `/api/stats` | StatsController |
| `export` | GET | `/api/data/export` | DataController |
| `import` | POST | `/api/data/import` | DataController |

---

## 3. Target Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Frontend (Browser)                          │
│              Thymeleaf Templates / React / Vue.js                   │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      REST Controllers Layer                         │
│   UserController, ProductController, CartController, OrderController│
│   PaymentController, ReviewController, AuthController               │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Service Layer (Existing)                     │
│   DefaultUserService, DefaultProductService, DefaultCartService     │
│   DefaultOrderService, DefaultPaymentService, DefaultReviewService  │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Repository Layer                               │
│   Option A: Keep InMemory*Repository (for development/testing)     │
│   Option B: Migrate to Spring Data JPA Repositories                │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Database                                      │
│              H2 (dev) / PostgreSQL / MySQL (prod)                   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. Dependencies Setup

### Modified `pom.xml`

Replace the current `pom.xml` with Spring Boot parent and dependencies:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot Parent -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>com.c2c</groupId>
    <artifactId>c2c-ecommerce</artifactId>
    <version>2.0.0</version>
    <name>C2C E-Commerce Platform</name>
    <description>Spring Boot GUI version of C2C marketplace</description>

    <properties>
        <java.version>21</java.version> <!-- Spring Boot 3.x requires Java 17+ -->
    </properties>

    <dependencies>
        <!-- Spring Boot Web (REST API + MVC) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Thymeleaf (Server-side templates) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>

        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Boot Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring Data JPA (optional - for database persistence) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- H2 Database (in-memory, for development) -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- PostgreSQL (for production) -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Jackson for JSON (already included in web starter) -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- Lombok (optional - reduces boilerplate) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- DevTools for hot reload -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 5. Project Structure Changes

### New Directory Structure

```
src/main/java/c2c/
├── C2CApplication.java              # Spring Boot entry point
├── config/
│   ├── SecurityConfig.java          # Spring Security configuration
│   ├── WebConfig.java                # CORS, interceptors
│   └── DataInitializer.java          # Demo data seeding (replaces seedDemoData)
├── controller/
│   ├── api/                          # REST API controllers
│   │   ├── UserApiController.java
│   │   ├── ProductApiController.java
│   │   ├── CartApiController.java
│   │   ├── OrderApiController.java
│   │   ├── PaymentApiController.java
│   │   ├── ReviewApiController.java
│   │   └── AuthApiController.java
│   └── web/                          # Thymeleaf view controllers
│       ├── HomeController.java
│       ├── ProductViewController.java
│       ├── CartViewController.java
│       └── OrderViewController.java
├── dto/                              # Data Transfer Objects
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── ProductCreateRequest.java
│   │   ├── CartAddRequest.java
│   │   ├── PaymentRequest.java
│   │   └── ReviewRequest.java
│   └── response/
│       ├── ApiResponse.java
│       ├── UserResponse.java
│       ├── ProductResponse.java
│       └── OrderResponse.java
├── exception/
│   ├── GlobalExceptionHandler.java   # @ControllerAdvice
│   └── ErrorResponse.java
├── user/                             # (existing package - keep as-is)
├── product/                          # (existing package - keep as-is)
├── cart/                             # (existing package - keep as-is)
├── order/                            # (existing package - keep as-is)
├── payment/                          # (existing package - keep as-is)
├── review/                           # (existing package - keep as-is)
├── bank/                             # (existing package - keep as-is)
├── common/                           # (existing package - keep as-is)
└── data/                             # (existing package - keep as-is)

src/main/resources/
├── application.yml                   # Spring Boot configuration
├── application-dev.yml               # Development profile
├── application-prod.yml              # Production profile
├── static/                           # Static assets (CSS, JS, images)
│   ├── css/
│   │   └── styles.css
│   ├── js/
│   │   └── app.js
│   └── images/
└── templates/                        # Thymeleaf templates
    ├── layout/
    │   └── main.html                 # Base layout
    ├── fragments/
    │   ├── header.html
    │   ├── footer.html
    │   └── nav.html
    ├── index.html                    # Home page
    ├── products/
    │   ├── list.html
    │   ├── detail.html
    │   └── create.html
    ├── cart/
    │   └── view.html
    ├── orders/
    │   ├── list.html
    │   └── detail.html
    ├── auth/
    │   ├── login.html
    │   └── register.html
    └── error/
        ├── 404.html
        └── 500.html
```

---

## 6. Spring Boot Configuration

### `application.yml`

```yaml
spring:
  application:
    name: c2c-ecommerce
  
  # Use in-memory H2 for development
  datasource:
    url: jdbc:h2:mem:c2cdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  # H2 Console (for debugging)
  h2:
    console:
      enabled: true
      path: /h2-console
  
  # Thymeleaf Configuration
  thymeleaf:
    cache: false  # Disable for development
    prefix: classpath:/templates/
    suffix: .html

# Server Configuration
server:
  port: 8080
  error:
    include-message: always
    include-binding-errors: always

# Logging
logging:
  level:
    c2c: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG

# Custom App Properties
app:
  demo-data:
    enabled: true
  jwt:
    secret: your-256-bit-secret-key-for-jwt-tokens
    expiration: 86400000  # 24 hours
```

### `C2CApplication.java` (Entry Point)

```java
package c2c;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class C2CApplication {
    public static void main(String[] args) {
        SpringApplication.run(C2CApplication.class, args);
    }
}
```

---

## 7. Repository Layer Migration

### Option A: Keep Existing In-Memory Repositories (Recommended for Phase 1)

Register existing repositories as Spring beans:

```java
package c2c.config;

import c2c.bank.*;
import c2c.cart.*;
import c2c.order.*;
import c2c.payment.*;
import c2c.product.*;
import c2c.review.*;
import c2c.user.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfig {

    @Bean
    public UserRepository userRepository() {
        return new InMemoryUserRepository();
    }

    @Bean
    public ProductRepository productRepository() {
        return new InMemoryProductRepository();
    }

    @Bean
    public CartRepository cartRepository() {
        return new InMemoryCartRepository();
    }

    @Bean
    public OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    public PaymentRepository paymentRepository() {
        return new InMemoryPaymentRepository();
    }

    @Bean
    public ReviewRepository reviewRepository() {
        return new InMemoryReviewRepository();
    }

    @Bean
    public TransactionLogRepository transactionLogRepository() {
        return new InMemoryTransactionLogRepository();
    }

    @Bean
    public TransactionLockManager transactionLockManager() {
        return new TransactionLockManager();
    }

    @Bean
    public PasswordHasher passwordHasher() {
        return new SimplePasswordHasher();
    }

    @Bean
    public FakeBankService bankService() {
        FakeBankService bankService = new FakeBankService();
        // Seed demo bank cards
        bankService.seedBalance("CARD-001", 10000.0);
        bankService.seedBalance("CARD-002", 5000.0);
        bankService.seedBalance("CARD-003", 2000.0);
        bankService.seedBalance("CARD-ALICE", 15000.0);
        bankService.seedBalance("CARD-BOB", 8000.0);
        bankService.seedBalance("CARD-CHARLIE", 3000.0);
        return bankService;
    }
}
```

### Option B: Migrate to Spring Data JPA (Full Migration)

Convert entities with JPA annotations:

```java
package c2c.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    private String id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private String phone;
    
    @Column(nullable = false)
    private String hashedPassword;
    
    @Column(nullable = false)
    private String name;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles;
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
    @Column(updatable = false)
    private Instant createdAt;
    
    // Existing getters and setters...
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
```

Create Spring Data JPA Repository:

```java
package c2c.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface JpaUserRepository extends JpaRepository<User, String>, UserRepository {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
```

---

## 8. Service Layer Migration

The existing services can be used **as-is** with minimal changes. Add `@Service` annotations:

```java
package c2c.user;

import c2c.common.Result;
import org.springframework.stereotype.Service;

@Service  // ADD THIS ANNOTATION
public class DefaultUserService implements UserService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    // Constructor injection works with Spring automatically
    public DefaultUserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    // Existing methods remain unchanged...
}
```

Apply the same pattern to all service classes:
- `DefaultProductService` → add `@Service`
- `DefaultCartService` → add `@Service`
- `DefaultOrderService` → add `@Service`
- `DefaultPaymentService` → add `@Service`
- `DefaultReviewService` → add `@Service`

---

## 9. REST Controller Implementation

### Base API Response Wrapper

```java
package c2c.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, Instant.now());
    }
}
```

### UserApiController

```java
package c2c.controller.api;

import c2c.common.Result;
import c2c.dto.request.RegisterRequest;
import c2c.dto.request.LoginRequest;
import c2c.dto.response.ApiResponse;
import c2c.dto.response.UserResponse;
import c2c.user.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserApiController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserApiController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // POST /api/users/register
    // Replaces CLI: register <email> <phone> <password> <name>
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        Result<User> result = userService.register(
            request.email(),
            request.phone(),
            request.password(),
            request.name()
        );
        UserResponse response = UserResponse.from(result.getData());
        return ResponseEntity.ok(ApiResponse.ok(response, "User registered successfully"));
    }

    // GET /api/users
    // Replaces CLI: users
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers() {
        List<UserResponse> users = userRepository.findAll()
            .stream()
            .map(UserResponse::from)
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    // GET /api/users/{id}
    // Replaces CLI: user <userId>
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found: " + id));
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user)));
    }

    // PUT /api/users/{id}
    // Replaces CLI: update-profile <userId> <name> [phone]
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable String id,
            @RequestBody UpdateProfileRequest request) {
        Result<User> result = userService.updateProfile(id, request.name(), request.phone());
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(result.getData())));
    }
}
```

### ProductApiController

```java
package c2c.controller.api;

import c2c.common.Result;
import c2c.dto.request.ProductCreateRequest;
import c2c.dto.response.ApiResponse;
import c2c.dto.response.ProductResponse;
import c2c.product.*;
import c2c.review.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;
    private final ReviewService reviewService;

    public ProductApiController(ProductService productService, ReviewService reviewService) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    // POST /api/products
    // Replaces CLI: add-product <sellerId> <title> <desc> <price> <stock> [category]
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        Result<Product> result = productService.create(
            request.sellerId(),
            request.title(),
            request.description(),
            request.price(),
            request.stock(),
            request.category()
        );
        return ResponseEntity.ok(ApiResponse.ok(ProductResponse.from(result.getData())));
    }

    // GET /api/products
    // Replaces CLI: products
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> listProducts() {
        Result<List<Product>> result = productService.list();
        List<ProductResponse> products = result.getData().stream()
            .map(ProductResponse::from)
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    // GET /api/products/{id}
    // Replaces CLI: product <productId>
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable String id) {
        Result<Product> result = productService.get(id);
        ProductResponse response = ProductResponse.from(result.getData());
        // Include rating
        double rating = reviewService.averageRating(id).getData();
        response = response.withRating(rating);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // GET /api/products/search?q=keyword
    // Replaces CLI: search <keyword>
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam("q") String keyword) {
        Result<List<Product>> result = productService.list();
        List<ProductResponse> matches = result.getData().stream()
            .filter(p -> p.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                        p.getDescription().toLowerCase().contains(keyword.toLowerCase()) ||
                        p.getCategory().toLowerCase().contains(keyword.toLowerCase()))
            .map(ProductResponse::from)
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(matches));
    }

    // PUT /api/products/{id}
    // Replaces CLI: update-product <productId> <title> <desc> <price> <stock> [category] [status]
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductUpdateRequest request) {
        Result<Product> result = productService.update(
            id,
            request.title(),
            request.description(),
            request.price(),
            request.stock(),
            request.category(),
            request.status()
        );
        return ResponseEntity.ok(ApiResponse.ok(ProductResponse.from(result.getData())));
    }
}
```

### CartApiController

```java
package c2c.controller.api;

import c2c.cart.*;
import c2c.common.Result;
import c2c.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    private final CartService cartService;

    public CartApiController(CartService cartService) {
        this.cartService = cartService;
    }

    // GET /api/cart/{userId}
    // Replaces CLI: cart <userId>
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<CartItem>>> viewCart(@PathVariable String userId) {
        Result<List<CartItem>> result = cartService.view(userId);
        return ResponseEntity.ok(ApiResponse.ok(result.getData()));
    }

    // POST /api/cart/{userId}/items
    // Replaces CLI: cart-add <userId> <productId> <quantity>
    @PostMapping("/{userId}/items")
    public ResponseEntity<ApiResponse<List<CartItem>>> addToCart(
            @PathVariable String userId,
            @RequestBody CartAddRequest request) {
        Result<List<CartItem>> result = cartService.addItem(
            userId, request.productId(), request.quantity()
        );
        return ResponseEntity.ok(ApiResponse.ok(result.getData(), "Item added to cart"));
    }

    // PUT /api/cart/{userId}/items/{productId}
    // Replaces CLI: cart-update <userId> <productId> <quantity>
    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<ApiResponse<List<CartItem>>> updateCartItem(
            @PathVariable String userId,
            @PathVariable String productId,
            @RequestBody CartUpdateRequest request) {
        Result<List<CartItem>> result = cartService.updateQuantity(
            userId, productId, request.quantity()
        );
        return ResponseEntity.ok(ApiResponse.ok(result.getData()));
    }

    // DELETE /api/cart/{userId}/items/{productId}
    // Replaces CLI: cart-remove <userId> <productId>
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<ApiResponse<List<CartItem>>> removeFromCart(
            @PathVariable String userId,
            @PathVariable String productId) {
        Result<List<CartItem>> result = cartService.removeItem(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok(result.getData(), "Item removed from cart"));
    }
}
```

### OrderApiController

```java
package c2c.controller.api;

import c2c.common.Result;
import c2c.dto.response.ApiResponse;
import c2c.dto.response.OrderResponse;
import c2c.order.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderApiController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    // POST /api/orders
    // Replaces CLI: checkout <userId>
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestBody CreateOrderRequest request) {
        Result<Order> result = orderService.createFromCart(request.buyerId());
        return ResponseEntity.ok(ApiResponse.ok(
            OrderResponse.from(result.getData()),
            "Order created successfully"
        ));
    }

    // GET /api/orders
    // Replaces CLI: orders [buyerId]
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listOrders(
            @RequestParam(required = false) String buyerId) {
        List<Order> orders;
        if (buyerId != null) {
            orders = orderService.listByBuyer(buyerId).getData();
        } else {
            orders = orderRepository.findAll();
        }
        List<OrderResponse> responses = orders.stream()
            .map(OrderResponse::from)
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    // GET /api/orders/{id}
    // Replaces CLI: order <orderId>
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String id) {
        Result<Order> result = orderService.get(id);
        return ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(result.getData())));
    }

    // PATCH /api/orders/{id}/status
    // Replaces CLI: order-status <orderId> <status>
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String id,
            @RequestBody UpdateStatusRequest request) {
        Result<Order> result = orderService.updateStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.ok(
            OrderResponse.from(result.getData()),
            "Order status updated to " + request.status()
        ));
    }
}
```

### PaymentApiController

```java
package c2c.controller.api;

import c2c.common.Result;
import c2c.dto.request.PaymentRequest;
import c2c.dto.response.ApiResponse;
import c2c.payment.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentApiController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    public PaymentApiController(PaymentService paymentService, PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }

    // POST /api/payments
    // Replaces CLI: pay <orderId> <method:DEBIT|CREDIT> <payerId> <cardNumber>
    @PostMapping
    public ResponseEntity<ApiResponse<Payment>> processPayment(@RequestBody PaymentRequest request) {
        Result<Payment> result = paymentService.pay(
            request.orderId(),
            request.method(),
            request.payerId(),
            request.cardNumber()
        );
        return ResponseEntity.ok(ApiResponse.ok(result.getData(), "Payment processed successfully"));
    }

    // GET /api/payments
    // Replaces CLI: payments [orderId]
    @GetMapping
    public ResponseEntity<ApiResponse<List<Payment>>> listPayments(
            @RequestParam(required = false) String orderId) {
        List<Payment> payments = paymentRepository.findAll();
        if (orderId != null) {
            payments = payments.stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .toList();
        }
        return ResponseEntity.ok(ApiResponse.ok(payments));
    }
}
```

### ReviewApiController

```java
package c2c.controller.api;

import c2c.common.Result;
import c2c.dto.request.ReviewRequest;
import c2c.dto.response.ApiResponse;
import c2c.review.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewApiController {

    private final ReviewService reviewService;

    public ReviewApiController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // POST /api/reviews
    // Replaces CLI: review <orderId> <productId> <buyerId> <rating:1-5> "<comment>"
    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<Review>> addReview(@RequestBody ReviewRequest request) {
        Result<Review> result = reviewService.addReview(
            request.orderId(),
            request.productId(),
            request.buyerId(),
            request.rating(),
            request.comment()
        );
        return ResponseEntity.ok(ApiResponse.ok(result.getData(), "Review added successfully"));
    }

    // GET /api/products/{productId}/reviews
    // Replaces CLI: reviews <productId>
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<List<Review>>> getProductReviews(@PathVariable String productId) {
        Result<List<Review>> result = reviewService.list(productId);
        return ResponseEntity.ok(ApiResponse.ok(result.getData()));
    }

    // GET /api/products/{productId}/rating
    // Replaces CLI: rating <productId>
    @GetMapping("/products/{productId}/rating")
    public ResponseEntity<ApiResponse<Double>> getProductRating(@PathVariable String productId) {
        Result<Double> result = reviewService.averageRating(productId);
        return ResponseEntity.ok(ApiResponse.ok(result.getData()));
    }
}
```

### Global Exception Handler

```java
package c2c.exception;

import c2c.common.exceptions.*;
import c2c.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(ValidationException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Internal server error: " + ex.getMessage()));
    }
}
```

---

## 10. Frontend Options

### Option A: Thymeleaf (Server-Side Rendering)

Example template for product listing:

```html
<!-- templates/products/list.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" th:replace="~{layout/main :: layout(~{::content})}">
<body>
<div th:fragment="content">
    <h1>Products</h1>
    
    <div class="search-bar">
        <form th:action="@{/products}" method="get">
            <input type="text" name="q" placeholder="Search products..." th:value="${keyword}">
            <button type="submit">Search</button>
        </form>
    </div>
    
    <div class="product-grid">
        <div th:each="product : ${products}" class="product-card">
            <h3 th:text="${product.title}">Product Title</h3>
            <p th:text="${product.description}">Description</p>
            <span class="price" th:text="'$' + ${#numbers.formatDecimal(product.price, 1, 2)}">$0.00</span>
            <span class="stock" th:text="'Stock: ' + ${product.stock}">Stock: 0</span>
            <a th:href="@{/products/{id}(id=${product.id})}" class="btn">View Details</a>
            <form th:action="@{/cart/add}" method="post" th:if="${product.stock > 0}">
                <input type="hidden" name="productId" th:value="${product.id}">
                <input type="number" name="quantity" value="1" min="1" th:max="${product.stock}">
                <button type="submit">Add to Cart</button>
            </form>
        </div>
    </div>
</div>
</body>
</html>
```

### Option B: React/Vue.js (SPA - Recommended for Rich UI)

Create a separate frontend project that consumes the REST API:

```javascript
// React example - ProductList.jsx
import { useState, useEffect } from 'react';
import axios from 'axios';

function ProductList() {
    const [products, setProducts] = useState([]);
    const [search, setSearch] = useState('');

    useEffect(() => {
        fetchProducts();
    }, []);

    const fetchProducts = async () => {
        const response = await axios.get('/api/products');
        setProducts(response.data.data);
    };

    const searchProducts = async (keyword) => {
        const response = await axios.get(`/api/products/search?q=${keyword}`);
        setProducts(response.data.data);
    };

    const addToCart = async (productId, quantity) => {
        await axios.post(`/api/cart/${currentUserId}/items`, {
            productId,
            quantity
        });
        alert('Added to cart!');
    };

    return (
        <div className="product-list">
            <input 
                type="text" 
                value={search}
                onChange={(e) => {
                    setSearch(e.target.value);
                    searchProducts(e.target.value);
                }}
                placeholder="Search products..."
            />
            
            <div className="grid">
                {products.map(product => (
                    <div key={product.id} className="product-card">
                        <h3>{product.title}</h3>
                        <p>{product.description}</p>
                        <span className="price">${product.price.toFixed(2)}</span>
                        <button onClick={() => addToCart(product.id, 1)}>
                            Add to Cart
                        </button>
                    </div>
                ))}
            </div>
        </div>
    );
}
```

---

## 11. Security Implementation

### Spring Security Configuration

```java
package c2c.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable for API (use tokens instead)
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/api/auth/**", "/api/products/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                // Authenticated endpoints
                .requestMatchers("/api/cart/**", "/api/orders/**", "/api/payments/**").authenticated()
                .requestMatchers("/api/users/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .headers(headers -> headers.frameOptions().sameOrigin()); // For H2 console
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### JWT Authentication (For REST API)

```java
package c2c.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

---

## 12. Migration Checklist

### Phase 1: Core Setup
- [ ] Update `pom.xml` with Spring Boot dependencies
- [ ] Create `C2CApplication.java` entry point
- [ ] Create `application.yml` configuration
- [ ] Create `RepositoryConfig.java` to register existing repositories as beans

### Phase 2: Service Layer
- [ ] Add `@Service` annotation to `DefaultUserService`
- [ ] Add `@Service` annotation to `DefaultProductService`
- [ ] Add `@Service` annotation to `DefaultCartService`
- [ ] Add `@Service` annotation to `DefaultOrderService`
- [ ] Add `@Service` annotation to `DefaultPaymentService`
- [ ] Add `@Service` annotation to `DefaultReviewService`

### Phase 3: REST Controllers
- [ ] Create DTOs (request/response objects)
- [ ] Implement `UserApiController`
- [ ] Implement `ProductApiController`
- [ ] Implement `CartApiController`
- [ ] Implement `OrderApiController`
- [ ] Implement `PaymentApiController`
- [ ] Implement `ReviewApiController`
- [ ] Implement `GlobalExceptionHandler`

### Phase 4: Frontend
- [ ] Create Thymeleaf templates OR React/Vue app
- [ ] Implement product listing page
- [ ] Implement product detail page
- [ ] Implement shopping cart page
- [ ] Implement checkout flow
- [ ] Implement order history page
- [ ] Implement user profile page

### Phase 5: Security
- [ ] Configure Spring Security
- [ ] Implement login/registration pages
- [ ] Add JWT authentication for API
- [ ] Secure endpoints based on roles

### Phase 6: Testing & Polish
- [ ] Write integration tests for controllers
- [ ] Add input validation
- [ ] Implement error pages
- [ ] Add logging
- [ ] Performance optimization

---

## 13. Sample Code Examples

### Request DTOs

```java
package c2c.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank @Email String email,
    String phone,
    @NotBlank @Size(min = 6) String password,
    @NotBlank String name
) {}

public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {}

public record ProductCreateRequest(
    @NotBlank String sellerId,
    @NotBlank String title,
    String description,
    @Positive double price,
    @PositiveOrZero int stock,
    String category
) {}

public record CartAddRequest(
    @NotBlank String productId,
    @Positive int quantity
) {}

public record PaymentRequest(
    @NotBlank String orderId,
    @NotNull PaymentMethod method,
    @NotBlank String payerId,
    @NotBlank String cardNumber
) {}

public record ReviewRequest(
    @NotBlank String orderId,
    @NotBlank String productId,
    @NotBlank String buyerId,
    @Min(1) @Max(5) int rating,
    String comment
) {}
```

### Response DTOs

```java
package c2c.dto.response;

import c2c.user.User;
import c2c.product.Product;
import c2c.order.Order;
import java.time.Instant;

public record UserResponse(
    String id,
    String email,
    String phone,
    String name,
    Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getPhone(),
            user.getName(),
            user.getCreatedAt()
        );
    }
}

public record ProductResponse(
    String id,
    String sellerId,
    String title,
    String description,
    double price,
    int stock,
    String category,
    String status,
    Double rating,
    Instant createdAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
            p.getId(), p.getSellerId(), p.getTitle(), p.getDescription(),
            p.getPrice(), p.getStock(), p.getCategory(), 
            p.getStatus().name(), null, p.getCreatedAt()
        );
    }
    
    public ProductResponse withRating(Double rating) {
        return new ProductResponse(
            id, sellerId, title, description, price, stock,
            category, status, rating, createdAt
        );
    }
}
```

### Data Initializer (Demo Data)

```java
package c2c.config;

import c2c.user.UserService;
import c2c.product.ProductService;
import c2c.cart.CartService;
import c2c.order.OrderService;
import c2c.payment.*;
import c2c.review.ReviewService;
import c2c.bank.FakeBankService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataInitializer {

    @Bean
    @Profile("dev") // Only run in development
    public CommandLineRunner initData(
            UserService userService,
            ProductService productService,
            CartService cartService,
            OrderService orderService,
            PaymentService paymentService,
            ReviewService reviewService,
            FakeBankService bankService) {
        
        return args -> {
            // Copy the seedDemoData() logic from Main.java
            var alice = userService.register("alice@example.com", "0912-345-678", "password123", "Alice Wang").getData();
            var bob = userService.register("bob@example.com", "0923-456-789", "password123", "Bob Chen").getData();
            // ... rest of demo data
            
            System.out.println("✓ Demo data initialized!");
        };
    }
}
```

---

## Running the Application

After migration, start the application:

```bash
# Development
mvn spring-boot:run

# Or with a specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production build
mvn clean package
java -jar target/c2c-ecommerce-2.0.0.jar --spring.profiles.active=prod
```

Access the application:
- Web UI: http://localhost:8080
- API: http://localhost:8080/api/products
- H2 Console: http://localhost:8080/h2-console

---

## Summary

This migration guide transforms the CLI-based C2C platform into a modern web application while **preserving the existing service layer logic**. The key benefits:

1. **Minimal Code Changes**: Existing services work with just `@Service` annotations
2. **RESTful API**: All CLI commands mapped to HTTP endpoints
3. **Flexible Frontend**: Choose Thymeleaf for SSR or React/Vue for SPA
4. **Production Ready**: Spring Security, JPA, and logging built-in
5. **Testable**: Spring Boot Test for integration testing

The architecture remains clean with clear separation of concerns, and the existing `Result<T>` pattern integrates seamlessly with REST responses.
