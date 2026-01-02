package c2c.web;

import c2c.bank.*;
import c2c.cart.*;
import c2c.order.*;
import c2c.payment.*;
import c2c.product.*;
import c2c.review.*;
import c2c.user.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot entry point for C2C E-Commerce Platform GUI.
 * Run with: mvn spring-boot:run
 * Access at: http://localhost:8080
 */
@SpringBootApplication
public class C2CApplication {

    public static void main(String[] args) {
        SpringApplication.run(C2CApplication.class, args);
    }

    // ===== Repositories =====
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

    // ===== Infrastructure =====
    @Bean
    public PasswordHasher passwordHasher() {
        return new SimplePasswordHasher();
    }

    @Bean
    public TransactionLockManager transactionLockManager() {
        return new TransactionLockManager();
    }

    @Bean
    public FakeBankService bankService() {
        FakeBankService bank = new FakeBankService();
        bank.seedBalance("CARD-001", 10000.0);
        bank.seedBalance("CARD-002", 5000.0);
        bank.seedBalance("CARD-ALICE", 15000.0);
        bank.seedBalance("CARD-BOB", 8000.0);
        return bank;
    }

    // ===== Services =====
    @Bean
    public UserService userService(UserRepository userRepository, PasswordHasher passwordHasher) {
        return new DefaultUserService(userRepository, passwordHasher);
    }

    @Bean
    public ProductService productService(ProductRepository productRepository) {
        return new DefaultProductService(productRepository);
    }

    @Bean
    public CartService cartService(CartRepository cartRepository, ProductRepository productRepository) {
        return new DefaultCartService(cartRepository, productRepository);
    }

    @Bean
    public OrderService orderService(OrderRepository orderRepository, CartRepository cartRepository,
            ProductRepository productRepository) {
        return new DefaultOrderService(orderRepository, cartRepository, productRepository);
    }

    @Bean
    public ReviewService reviewService(ReviewRepository reviewRepository, OrderRepository orderRepository) {
        return new DefaultReviewService(reviewRepository, orderRepository);
    }

    @Bean
    public PaymentService paymentService(OrderRepository orderRepository, PaymentRepository paymentRepository,
            FakeBankService bankService, TransactionLockManager lockManager,
            TransactionLogRepository logRepository) {
        return new DefaultPaymentService(orderRepository, paymentRepository, bankService, lockManager, logRepository,
                500);
    }
}
