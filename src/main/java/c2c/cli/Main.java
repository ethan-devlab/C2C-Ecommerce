package c2c.cli;

import c2c.bank.FakeBankService;
import c2c.bank.InMemoryTransactionLogRepository;
import c2c.bank.TransactionLockManager;
import c2c.cart.DefaultCartService;
import c2c.cart.InMemoryCartRepository;
import c2c.cart.CartService;
import c2c.common.Result;
import c2c.data.DataPersistenceService;
import c2c.data.DataSnapshot;
import c2c.order.DefaultOrderService;
import c2c.order.InMemoryOrderRepository;
import c2c.order.Order;
import c2c.order.OrderRepository;
import c2c.order.OrderStatus;
import c2c.payment.DefaultPaymentService;
import c2c.payment.InMemoryPaymentRepository;
import c2c.payment.Payment;
import c2c.payment.PaymentMethod;
import c2c.payment.PaymentRepository;
import c2c.payment.PaymentService;
import c2c.product.DefaultProductService;
import c2c.product.InMemoryProductRepository;
import c2c.product.Product;
import c2c.product.ProductRepository;
import c2c.review.DefaultReviewService;
import c2c.review.InMemoryReviewRepository;
import c2c.review.Review;
import c2c.review.ReviewRepository;
import c2c.review.ReviewService;
import c2c.user.DefaultUserService;
import c2c.user.InMemoryUserRepository;
import c2c.user.PasswordHasher;
import c2c.user.SimplePasswordHasher;
import c2c.user.User;
import c2c.user.UserRepository;
import c2c.user.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Minimal CLI shell to exercise core services. Not production ready; intended
 * for quick smoke checks.
 */
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        UserRepository userRepository = new InMemoryUserRepository();
        PasswordHasher passwordHasher = new SimplePasswordHasher();
        UserService userService = new DefaultUserService(userRepository, passwordHasher);

        ProductRepository productRepository = new InMemoryProductRepository();
        DefaultProductService productService = new DefaultProductService(productRepository);

        InMemoryCartRepository cartRepository = new InMemoryCartRepository();
        CartService cartService = new DefaultCartService(cartRepository, productRepository);

        OrderRepository orderRepository = new InMemoryOrderRepository();
        DefaultOrderService orderService = new DefaultOrderService(orderRepository, cartRepository, productRepository);

        ReviewRepository reviewRepository = new InMemoryReviewRepository();
        ReviewService reviewService = new DefaultReviewService(reviewRepository, orderRepository);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-1", 1000);
        TransactionLockManager lockManager = new TransactionLockManager();
        InMemoryTransactionLogRepository logRepository = new InMemoryTransactionLogRepository();
        PaymentRepository paymentRepository = new InMemoryPaymentRepository();
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepository, 500);

        logger.info("C2C CLI started. Type 'help' for commands.");
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        System.out.print("> ");
        while (running && scanner.hasNextLine()) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(" ");
            String cmd = parts[0];
            try {
                switch (cmd) {
                    case "help" -> printHelp();
                    case "register" -> handleRegister(userService, parts);
                    case "add-product" -> handleAddProduct(productService, parts);
                    case "list-products" -> handleListProducts(productService);
                    case "list-orders" -> handleListOrders(orderRepository);
                    case "list-payments" -> handleListPayments(paymentRepository);
                    case "list-reviews" -> handleListReviews(reviewService, parts);
                    case "cart-add" -> handleCartAdd(cartService, parts);
                    case "order-create" -> handleOrderCreate(orderService, parts);
                    case "order-status" -> handleOrderStatus(orderService, parts);
                    case "pay" -> handlePay(paymentService, parts);
                    case "export" -> handleExport(parts, userRepository, productRepository, orderRepository,
                            paymentRepository, reviewRepository);
                    case "import" -> handleImport(parts, userRepository, productRepository, orderRepository,
                            paymentRepository, reviewRepository);
                    case "exit" -> running = false;
                    default -> System.out.println("Unknown command. Type 'help'.");
                }
            } catch (Exception ex) {
                logger.error("Command failed: {}", cmd, ex);
                System.out.println("Error: " + ex.getMessage());
            }
        }
        logger.info("CLI exiting.");
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  register <email> <password> [name]");
        System.out.println("  add-product <sellerId> <title> <price> <stock>");
        System.out.println("  list-products");
        System.out.println("  list-orders [buyerId]");
        System.out.println("  list-payments");
        System.out.println("  list-reviews <productId>");
        System.out.println("  cart-add <userId> <productId> <qty>");
        System.out.println("  order-create <userId>");
        System.out.println("  order-status <orderId> <PENDING|PAID|SHIPPED|COMPLETED|CANCELED|REFUNDED>");
        System.out.println("  pay <orderId> <userId> <cardNumber>");
        System.out.println("  export <filePath>");
        System.out.println("  import <filePath>");
        System.out.println("  exit");
        System.out.print("> ");

    }

    private static void handleRegister(UserService userService, String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("usage: register <email> <password> [name]");
        }
        String email = parts[1];
        String password = parts[2];
        String name = parts.length > 3 ? parts[3] : email;
        Result<User> result = userService.register(email, null, password, name);
        System.out.println("registered: " + result.getData().getId());
    }

    private static void handleAddProduct(DefaultProductService productService, String[] parts) {
        if (parts.length < 5) {
            throw new IllegalArgumentException("usage: add-product <sellerId> <title> <price> <stock>");
        }
        String sellerId = parts[1];
        String title = parts[2];
        double price = Double.parseDouble(parts[3]);
        int stock = Integer.parseInt(parts[4]);
        Result<Product> result = productService.create(sellerId, title, "", price, stock, "general");
        System.out.println("product: " + result.getData().getId());
    }

    private static void handleListProducts(DefaultProductService productService) {
        Result<List<Product>> result = productService.list();
        for (Product p : result.getData()) {
            System.out.println(p.getId() + " " + p.getTitle() + " $" + p.getPrice() + " stock=" + p.getStock());
        }
    }

    private static void handleCartAdd(CartService cartService, String[] parts) {
        if (parts.length < 4) {
            throw new IllegalArgumentException("usage: cart-add <userId> <productId> <qty>");
        }
        String userId = parts[1];
        String productId = parts[2];
        int qty = Integer.parseInt(parts[3]);
        cartService.addItem(userId, productId, qty);
        System.out.println("cart updated");
    }

    private static void handleOrderCreate(DefaultOrderService orderService, String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("usage: order-create <userId>");
        }
        String userId = parts[1];
        Result<Order> orderResult = orderService.createFromCart(userId);
        System.out.println("order: " + orderResult.getData().getId() + " amount=" + orderResult.getData().getAmount());
    }

    private static void handleOrderStatus(DefaultOrderService orderService, String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "usage: order-status <orderId> <PENDING|PAID|SHIPPED|COMPLETED|CANCELED|REFUNDED>");
        }
        String orderId = parts[1];
        OrderStatus status = OrderStatus.valueOf(parts[2]);
        Result<Order> result = orderService.updateStatus(orderId, status);
        System.out.println("order status updated: " + result.getData().getStatus());
    }

    private static void handlePay(PaymentService paymentService, String[] parts) {
        if (parts.length < 4) {
            throw new IllegalArgumentException("usage: pay <orderId> <userId> <cardNumber>");
        }
        String orderId = parts[1];
        String userId = parts[2];
        String card = parts[3];
        Result<Payment> paymentResult = paymentService.pay(orderId, PaymentMethod.DEBIT, userId, card);
        System.out
                .println("paid: " + paymentResult.getData().getId() + " status=" + paymentResult.getData().getStatus());
    }

    private static void handleListOrders(OrderRepository orderRepository) {
        for (Order o : orderRepository.findAll()) {
            System.out.println(o.getId() + " buyer=" + o.getBuyerId() + " status=" + o.getStatus() + " amount="
                    + o.getAmount());
        }
    }

    private static void handleListPayments(PaymentRepository paymentRepository) {
        for (Payment p : paymentRepository.findAll()) {
            System.out.println(p.getId() + " order=" + p.getOrderId() + " status=" + p.getStatus() + " amt="
                    + p.getAmount());
        }
    }

    private static void handleListReviews(ReviewService reviewService, String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("usage: list-reviews <productId>");
        }
        String productId = parts[1];
        Result<List<Review>> result = reviewService.list(productId);
        for (Review r : result.getData()) {
            System.out.println(r.getId() + " rating=" + r.getRating() + " by=" + r.getBuyerId() + " comment="
                    + r.getComment());
        }
    }

    private static void handleExport(String[] parts, UserRepository userRepository, ProductRepository productRepository,
            OrderRepository orderRepository, PaymentRepository paymentRepository, ReviewRepository reviewRepository) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("usage: export <filePath>");
        }
        Path path = Paths.get(parts[1]);
        DataSnapshot snapshot = DataPersistenceService.exportAll(userRepository, productRepository, orderRepository,
                paymentRepository, reviewRepository);
        try {
            DataPersistenceService.writeToFile(snapshot, path);
            System.out.println("exported to " + path.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("export failed: " + e.getMessage(), e);
        }
    }

    private static void handleImport(String[] parts, UserRepository userRepository, ProductRepository productRepository,
            OrderRepository orderRepository, PaymentRepository paymentRepository, ReviewRepository reviewRepository) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("usage: import <filePath>");
        }
        Path path = Paths.get(parts[1]);
        try {
            DataSnapshot snapshot = DataPersistenceService.readFromFile(path);
            DataPersistenceService.importAll(snapshot, userRepository, productRepository, orderRepository,
                    paymentRepository, reviewRepository);
            System.out.println("imported from " + path.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("import failed: " + e.getMessage(), e);
        }
    }
}
