package c2c.web;

import c2c.cart.CartService;
import c2c.order.OrderService;
import c2c.order.OrderStatus;
import c2c.payment.PaymentMethod;
import c2c.payment.PaymentService;
import c2c.product.ProductService;
import c2c.review.ReviewService;
import c2c.user.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds demo data on application startup.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserService userService;
    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ReviewService reviewService;

    public DataSeeder(UserService userService, ProductService productService,
            CartService cartService, OrderService orderService,
            PaymentService paymentService, ReviewService reviewService) {
        this.userService = userService;
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.reviewService = reviewService;
    }

    @Override
    public void run(String... args) {
        // Create demo users
        var alice = userService.register("alice@example.com", "0912-345-678", "password123", "Alice Wang").getData();
        var bob = userService.register("bob@example.com", "0923-456-789", "password123", "Bob Chen").getData();
        var charlie = userService.register("charlie@example.com", "0934-567-890", "password123", "Charlie Lin")
                .getData();

        // Create demo products
        var laptop = productService
                .create(alice.getId(), "MacBook Pro 14", "Apple M3 Pro chip", 1999.99, 10, "Electronics").getData();
        var phone = productService.create(alice.getId(), "iPhone 15 Pro", "256GB, Titanium", 1199.00, 25, "Electronics")
                .getData();
        var book = productService.create(bob.getId(), "Clean Code", "Software craftsmanship book", 45.99, 100, "Books")
                .getData();
        var shirt = productService
                .create(charlie.getId(), "Cotton T-Shirt", "100% organic cotton", 29.99, 200, "Clothing").getData();

        // Create a completed order with review
        cartService.addItem(charlie.getId(), laptop.getId(), 1);
        var order = orderService.createFromCart(charlie.getId()).getData();
        paymentService.pay(order.getId(), PaymentMethod.CREDIT, charlie.getId(), "CARD-001");
        orderService.updateStatus(order.getId(), OrderStatus.SHIPPED);
        orderService.updateStatus(order.getId(), OrderStatus.COMPLETED);
        reviewService.addReview(order.getId(), laptop.getId(), charlie.getId(), 5, "Amazing laptop!");

        System.out.println("\n=== C2C Platform Started ===");
        System.out.println("Access at: http://localhost:8080");
        System.out.println("Demo users: Alice, Bob, Charlie");
        System.out.println("Demo products: 4 items");
        System.out.println("============================\n");
    }
}
