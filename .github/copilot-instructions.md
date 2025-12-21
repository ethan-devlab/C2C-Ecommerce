# C2C E-Commerce Platform - AI Coding Agent Guide

## Architecture Overview

This is a **layered Java 23 Maven project** implementing a C2C e-commerce platform with in-memory persistence. The architecture follows dependency injection patterns without a framework:

- **CLI Layer** (`c2c.cli`): Command parsing and user interaction
- **Service Layer** (`c2c.*`): Business logic in domain-specific packages (user, product, cart, order, payment, bank, review)
- **Repository Layer**: In-memory implementations (`InMemory*Repository`) - all inject-able via constructor
- **Infrastructure** (`c2c.common`, `c2c.data`, `c2c.bank`): Cross-cutting concerns (ID generation, Result wrappers, exceptions, locking, persistence)

Key architectural decision: **No Spring/DI framework** - all dependencies wired manually in [Main.java](src/main/java/c2c/cli/Main.java#L48-L77).

## Critical Concurrency Pattern: Transaction Locking

The payment flow uses **optimistic locking via `TransactionLockManager`**:

```java
String lockKey = payerId + "::" + cardNumber;
boolean locked = lockManager.tryLock(lockKey, lockTimeoutMillis);
try {
    // debit operation
} finally {
    lockManager.unlock(lockKey);
}
```

- Lock keys prevent concurrent transactions for same user+card
- Always use try-finally for lock release
- See [DefaultPaymentService](src/main/java/c2c/payment/DefaultPaymentService.java#L42-L80) for reference implementation

## Service Layer Patterns

### Result Wrapper Instead of Exceptions for Success Cases
Services return `Result<T>` for operations that may fail gracefully:
```java
Result<Order> result = orderService.createFromCart(buyerId);
if (result.isSuccess()) {
    Order order = result.getData();
}
```

Domain exceptions (`ValidationException`, `NotFoundException`) are thrown for exceptional cases but caught at CLI boundary.

### State Machines for Domain Transitions
Order status transitions are validated through `OrderStatusMachine.canTransit(from, to)` - **never modify status directly**. Example valid flows:
- `PENDING → PAID → SHIPPED → COMPLETED`
- `PENDING → CANCELED`
- `PAID → REFUNDED`

See [OrderStatusMachine](src/main/java/c2c/order/OrderStatusMachine.java) for all allowed transitions.

### Cross-Service Dependencies
Services orchestrate through constructor injection:
- `DefaultOrderService` depends on `CartRepository`, `ProductRepository`
- `DefaultPaymentService` depends on `OrderRepository`, `BankService`, `TransactionLockManager`
- Order creation decrements product stock directly in [DefaultOrderService](src/main/java/c2c/order/DefaultOrderService.java#L36-L46)

## Testing Strategy

### Unit Tests Use Direct Instantiation
No mocking framework for repositories - use in-memory implementations:
```java
ProductRepository productRepository = new InMemoryProductRepository();
OrderRepository orderRepository = new InMemoryOrderRepository();
```

See [PaymentServiceTest](src/test/java/c2c/payment/PaymentServiceTest.java#L44-L68) for orchestration pattern.

### Testing Concurrent Scenarios
Use `CountDownLatch` for coordinating threads in concurrency tests (see payment lock tests).

### Static ByteBuddy Configuration
Tests include `System.setProperty("net.bytebuddy.experimental", "true")` for Java 23 compatibility.

## Development Workflows

### Build and Test
```bash
mvn clean test                    # Run tests
mvn verify                        # Run tests + JaCoCo coverage report
mvn compile exec:java -Dexec.mainClass="c2c.cli.Main"  # Run CLI
```

Coverage reports: `target/site/jacoco/index.html`

### CLI Commands for Manual Testing
```
register <email> <phone> <pwd> <name>
add-product <sellerId> <title> <desc> <price> <stock> <cat>
cart-add <userId> <productId> <qty>
order-create <buyerId>
pay <orderId> <DEBIT|CREDIT> <payerId> <cardNo>
review-add <orderId> <productId> <buyerId> <1-5> <comment>
```

Bank seeding in Main: `bankService.seedBalance("CARD-1", 1000)` for test cards.

### Data Persistence (Optional)
`DataPersistenceService` provides JSON serialization:
- Export: `DataSnapshot snapshot = DataPersistenceService.exportAll(repos...)`
- Import: `DataPersistenceService.importAll(snapshot, repos...)`
- Sample data: [sample-data.json](src/main/resources/sample-data.json)

## Code Conventions

### ID Generation
Always use `IdGenerator.nextNumeric()` for entity IDs - returns String format.

### Logging
Log4j2 configured in [log4j2.xml](src/main/resources/log4j2.xml). Use:
```java
private static final Logger logger = LogManager.getLogger(ClassName.class);
logger.info("event description");
```

### Exception Hierarchy
- `ValidationException`: Business rule violations (insufficient stock, invalid transitions)
- `NotFoundException`: Entity not found in repository
- `ConflictException`: Duplicate email/phone registration

Throw these directly in services; CLI layer formats user-friendly messages.

### Repository Contracts
All repositories expose:
- `save(Entity)`: Upsert operation
- `findById(String id)`: Returns `Optional<Entity>`
- `findAll()`: Returns all entities

## Documentation References

- Architecture details: [ArchitectureDesign.md](docs/ArchitectureDesign.md)
- System design: [SystemDesign.md](docs/SystemDesign.md)
- Requirements: [SRS.md](docs/SRS.md)

## Common Pitfalls

1. **Don't bypass OrderStatusMachine** - always validate transitions
2. **Lock timeout handling** - payment locks default to 500ms, tune via constructor param
3. **Stock management** - order creation immediately decrements stock; cancellation does NOT restore
4. **Repository independence** - no cascading deletes/saves; services must coordinate
