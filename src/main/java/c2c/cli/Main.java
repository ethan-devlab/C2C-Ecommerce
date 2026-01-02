package c2c.cli;

import c2c.bank.FakeBankService;
import c2c.bank.InMemoryTransactionLogRepository;
import c2c.bank.TransactionLockManager;
import c2c.cart.CartItem;
import c2c.cart.DefaultCartService;
import c2c.cart.InMemoryCartRepository;
import c2c.cart.CartService;
import c2c.common.Result;
import c2c.common.exceptions.ConflictException;
import c2c.common.exceptions.NotFoundException;
import c2c.common.exceptions.ValidationException;
import c2c.data.DataPersistenceService;
import c2c.data.DataSnapshot;
import c2c.order.DefaultOrderService;
import c2c.order.InMemoryOrderRepository;
import c2c.order.Order;
import c2c.order.OrderItem;
import c2c.order.OrderRepository;
import c2c.order.OrderService;
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
import c2c.product.ProductService;
import c2c.product.ProductStatus;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * C2C E-Commerce Platform CLI
 * A comprehensive command-line interface for managing users, products, carts,
 * orders, payments, and reviews.
 */
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    // Services
    private static UserService userService;
    private static ProductService productService;
    private static CartService cartService;
    private static OrderService orderService;
    private static ReviewService reviewService;
    private static PaymentService paymentService;
    private static FakeBankService bankService;

    // Repositories (for direct access when needed)
    private static UserRepository userRepository;
    private static ProductRepository productRepository;
    private static OrderRepository orderRepository;
    private static PaymentRepository paymentRepository;
    private static ReviewRepository reviewRepository;

    public static void main(String[] args) {
        initializeServices();
        seedDemoData();
        printWelcome();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        printPrompt();
        while (running && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] parts = parseCommand(line);
            String cmd = parts[0].toLowerCase();

            try {
                running = executeCommand(cmd, parts);
            } catch (ValidationException e) {
                printError("Validation Error: " + e.getMessage());
            } catch (NotFoundException e) {
                printError("Not Found: " + e.getMessage());
            } catch (ConflictException e) {
                printError("Conflict: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                printError("Invalid Input: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Command failed: {}", cmd, e);
                printError("Error: " + e.getMessage());
            }

            if (running) {
                printPrompt();
            }
        }

        printInfo("Goodbye! Thank you for using C2C Platform.");
        logger.info("CLI exiting.");
    }

    private static void initializeServices() {
        userRepository = new InMemoryUserRepository();
        PasswordHasher passwordHasher = new SimplePasswordHasher();
        userService = new DefaultUserService(userRepository, passwordHasher);

        productRepository = new InMemoryProductRepository();
        productService = new DefaultProductService(productRepository);

        InMemoryCartRepository cartRepository = new InMemoryCartRepository();
        cartService = new DefaultCartService(cartRepository, productRepository);

        orderRepository = new InMemoryOrderRepository();
        orderService = new DefaultOrderService(orderRepository, cartRepository, productRepository);

        reviewRepository = new InMemoryReviewRepository();
        reviewService = new DefaultReviewService(reviewRepository, orderRepository);

        bankService = new FakeBankService();
        TransactionLockManager lockManager = new TransactionLockManager();
        InMemoryTransactionLogRepository logRepository = new InMemoryTransactionLogRepository();
        paymentRepository = new InMemoryPaymentRepository();
        paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepository, 500);

        logger.info("C2C CLI services initialized.");
    }

    private static void seedDemoData() {
        // Seed bank cards with balance
        bankService.seedBalance("CARD-001", 10000.0);
        bankService.seedBalance("CARD-002", 5000.0);
        bankService.seedBalance("CARD-003", 2000.0);
        bankService.seedBalance("CARD-ALICE", 15000.0);
        bankService.seedBalance("CARD-BOB", 8000.0);
        bankService.seedBalance("CARD-CHARLIE", 3000.0);

        // Create demo users
        var alice = userService.register("alice@example.com", "0912-345-678", "password123", "Alice Wang").getData();
        var bob = userService.register("bob@example.com", "0923-456-789", "password123", "Bob Chen").getData();
        var charlie = userService.register("charlie@example.com", "0934-567-890", "password123", "Charlie Lin")
                .getData();
        var david = userService.register("david@example.com", "0945-678-901", "password123", "David Lee").getData();
        var emma = userService.register("emma@example.com", "0956-789-012", "password123", "Emma Wu").getData();

        // Create demo products (Alice and Bob are sellers)
        var laptop = productService.create(alice.getId(), "MacBook Pro 14", "Apple M3 Pro chip, 18GB RAM, 512GB SSD",
                1999.99, 10, "Electronics").getData();
        var phone = productService
                .create(alice.getId(), "iPhone 15 Pro", "256GB, Natural Titanium", 1199.00, 25, "Electronics")
                .getData();
        var headphones = productService
                .create(alice.getId(), "AirPods Pro 2", "Active Noise Cancellation, USB-C", 249.00, 50, "Electronics")
                .getData();
        var watch = productService.create(alice.getId(), "Apple Watch Ultra 2", "49mm, GPS + Cellular, Titanium Case",
                799.00, 15, "Electronics").getData();

        var book1 = productService.create(bob.getId(), "Clean Code",
                "A Handbook of Agile Software Craftsmanship by Robert C. Martin", 45.99, 100, "Books").getData();
        var book2 = productService.create(bob.getId(), "Design Patterns",
                "Elements of Reusable Object-Oriented Software", 59.99, 80, "Books").getData();
        var book3 = productService
                .create(bob.getId(), "The Pragmatic Programmer", "20th Anniversary Edition", 49.99, 60, "Books")
                .getData();

        var shirt = productService.create(charlie.getId(), "Cotton T-Shirt",
                "Premium quality, 100% organic cotton, Black", 29.99, 200, "Clothing").getData();
        var jeans = productService
                .create(charlie.getId(), "Slim Fit Jeans", "Stretch denim, Dark blue wash", 79.99, 150, "Clothing")
                .getData();
        var jacket = productService
                .create(charlie.getId(), "Winter Jacket", "Waterproof, Windproof, with Hood", 149.99, 40, "Clothing")
                .getData();

        // Create demo orders and payments (Charlie, David, Emma are buyers)
        // Order 1: Charlie buys electronics from Alice
        cartService.addItem(charlie.getId(), laptop.getId(), 1);
        cartService.addItem(charlie.getId(), headphones.getId(), 1);
        var order1 = orderService.createFromCart(charlie.getId()).getData();
        paymentService.pay(order1.getId(), PaymentMethod.CREDIT, charlie.getId(), "CARD-CHARLIE");
        orderService.updateStatus(order1.getId(), OrderStatus.SHIPPED);
        orderService.updateStatus(order1.getId(), OrderStatus.COMPLETED);

        // Order 2: David buys books from Bob
        cartService.addItem(david.getId(), book1.getId(), 2);
        cartService.addItem(david.getId(), book2.getId(), 1);
        var order2 = orderService.createFromCart(david.getId()).getData();
        paymentService.pay(order2.getId(), PaymentMethod.DEBIT, david.getId(), "CARD-002");
        orderService.updateStatus(order2.getId(), OrderStatus.SHIPPED);
        orderService.updateStatus(order2.getId(), OrderStatus.COMPLETED);

        // Order 3: Emma buys clothing from Charlie
        cartService.addItem(emma.getId(), shirt.getId(), 3);
        cartService.addItem(emma.getId(), jeans.getId(), 2);
        var order3 = orderService.createFromCart(emma.getId()).getData();
        paymentService.pay(order3.getId(), PaymentMethod.CREDIT, emma.getId(), "CARD-001");
        orderService.updateStatus(order3.getId(), OrderStatus.SHIPPED);
        orderService.updateStatus(order3.getId(), OrderStatus.COMPLETED);

        // Order 4: Alice buys a book (pending payment)
        cartService.addItem(alice.getId(), book3.getId(), 1);
        var order4 = orderService.createFromCart(alice.getId()).getData();
        // Not paid yet - stays PENDING

        // Order 5: Bob buys phone (paid, awaiting shipment)
        cartService.addItem(bob.getId(), phone.getId(), 1);
        var order5 = orderService.createFromCart(bob.getId()).getData();
        paymentService.pay(order5.getId(), PaymentMethod.DEBIT, bob.getId(), "CARD-BOB");
        // Paid but not shipped yet

        // Create demo reviews (only for completed orders)
        reviewService.addReview(order1.getId(), laptop.getId(), charlie.getId(), 5,
                "Amazing laptop! Super fast and the display is gorgeous.");
        reviewService.addReview(order1.getId(), headphones.getId(), charlie.getId(), 4,
                "Great sound quality, but a bit pricey.");

        reviewService.addReview(order2.getId(), book1.getId(), david.getId(), 5,
                "Must-read for every developer. Changed how I write code!");
        reviewService.addReview(order2.getId(), book2.getId(), david.getId(), 5,
                "Classic book on design patterns. Essential reference.");

        reviewService.addReview(order3.getId(), shirt.getId(), emma.getId(), 4,
                "Comfortable and fits well. Good quality for the price.");
        reviewService.addReview(order3.getId(), jeans.getId(), emma.getId(), 3,
                "Decent jeans but runs a bit small. Consider sizing up.");

        logger.info("Demo data seeded: 5 users, 10 products, 5 orders, 4 payments, 6 reviews.");
    }

    private static String[] parseCommand(String line) {
        // Handle quoted strings for arguments with spaces
        java.util.List<String> tokens = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)").matcher(line);
        while (m.find()) {
            if (m.group(1) != null) {
                tokens.add(m.group(1));
            } else if (m.group(2) != null) {
                tokens.add(m.group(2));
            } else {
                tokens.add(m.group(3));
            }
        }
        return tokens.toArray(new String[0]);
    }

    private static boolean executeCommand(String cmd, String[] parts) {
        switch (cmd) {
            // General
            case "help", "h", "?" -> printHelp();
            case "exit", "quit", "q" -> {
                return false;
            }
            case "clear", "cls" -> clearScreen();

            // User Management
            case "register" -> handleRegister(parts);
            case "login" -> handleLogin(parts);
            case "users" -> handleListUsers();
            case "user" -> handleGetUser(parts);
            case "update-profile" -> handleUpdateProfile(parts);

            // Product Management
            case "add-product", "create-product" -> handleAddProduct(parts);
            case "update-product" -> handleUpdateProduct(parts);
            case "products", "list-products" -> handleListProducts();
            case "product" -> handleGetProduct(parts);
            case "search" -> handleSearchProducts(parts);

            // Cart Management
            case "cart" -> handleViewCart(parts);
            case "cart-add", "add-to-cart" -> handleCartAdd(parts);
            case "cart-update" -> handleCartUpdate(parts);
            case "cart-remove" -> handleCartRemove(parts);

            // Order Management
            case "checkout", "order-create" -> handleOrderCreate(parts);
            case "orders", "list-orders" -> handleListOrders(parts);
            case "order" -> handleGetOrder(parts);
            case "order-status", "update-order" -> handleOrderStatus(parts);

            // Payment
            case "pay", "payment" -> handlePay(parts);
            case "payments", "list-payments" -> handleListPayments(parts);
            case "seed-balance" -> handleSeedBalance(parts);

            // Review Management
            case "review", "add-review" -> handleAddReview(parts);
            case "reviews", "list-reviews" -> handleListReviews(parts);
            case "rating" -> handleGetRating(parts);

            // Data Management
            case "export" -> handleExport(parts);
            case "import" -> handleImport(parts);
            case "stats" -> handleStats();

            default -> {
                printError("Unknown command: '" + cmd + "'. Type 'help' for available commands.");
            }
        }
        return true;
    }

    // ==================== Output Helpers ====================

    private static void printWelcome() {
        System.out.println();
        System.out.println(ANSI_CYAN + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "║" + ANSI_BOLD + "           C2C E-Commerce Platform v1.0                   "
                + ANSI_RESET + ANSI_CYAN + "║" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "║" + "     Your trusted marketplace for buying and selling      " + ANSI_CYAN
                + "║" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        System.out.println();
        System.out.println("Type " + ANSI_YELLOW + "help" + ANSI_RESET + " to see available commands.");
        System.out.println();
        System.out.println(ANSI_YELLOW + "Demo Data Loaded:" + ANSI_RESET);
        System.out.println("  • 5 Users: Alice (seller), Bob (seller), Charlie, David, Emma");
        System.out.println("  • 10 Products across Electronics, Books, and Clothing");
        System.out.println("  • 5 Orders (3 completed, 1 pending payment, 1 awaiting shipment)");
        System.out.println("  • Bank Cards: CARD-001~003, CARD-ALICE, CARD-BOB, CARD-CHARLIE");
        System.out.println();
        System.out.println("Try: " + ANSI_CYAN + "users" + ANSI_RESET + ", " +
                ANSI_CYAN + "products" + ANSI_RESET + ", " +
                ANSI_CYAN + "orders" + ANSI_RESET + ", " +
                ANSI_CYAN + "stats" + ANSI_RESET);
        System.out.println();
    }

    private static void printPrompt() {
        System.out.print(ANSI_GREEN + "c2c" + ANSI_RESET + " > ");
        System.out.flush();
    }

    private static void printSuccess(String message) {
        System.out.println(ANSI_GREEN + "✓ " + message + ANSI_RESET);
    }

    private static void printError(String message) {
        System.out.println(ANSI_RED + "✗ " + message + ANSI_RESET);
    }

    private static void printInfo(String message) {
        System.out.println(ANSI_CYAN + "ℹ " + message + ANSI_RESET);
    }

    private static void printWarning(String message) {
        System.out.println(ANSI_YELLOW + "⚠ " + message + ANSI_RESET);
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println(ANSI_BOLD + "═══ " + title + " ═══" + ANSI_RESET);
    }

    private static void printTableHeader(String... columns) {
        StringBuilder sb = new StringBuilder();
        for (String col : columns) {
            sb.append(String.format("%-15s", col));
        }
        System.out.println(ANSI_BOLD + sb.toString() + ANSI_RESET);
        System.out.println("─".repeat(columns.length * 15));
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    // ==================== Help ====================

    private static void printHelp() {
        printHeader("Available Commands");

        System.out.println(ANSI_YELLOW + "\n📋 General:" + ANSI_RESET);
        System.out.println("  help, h, ?                         Show this help message");
        System.out.println("  clear, cls                         Clear the screen");
        System.out.println("  exit, quit, q                      Exit the application");
        System.out.println("  stats                              Show platform statistics");

        System.out.println(ANSI_YELLOW + "\n👤 User Management:" + ANSI_RESET);
        System.out.println("  register <email> <password> [name] [phone]");
        System.out.println("                                     Register a new user");
        System.out.println("  login <email> <password>           Login and verify credentials");
        System.out.println("  users                              List all users");
        System.out.println("  user <userId>                      Get user details");
        System.out.println("  update-profile <userId> <name> [phone]");
        System.out.println("                                     Update user profile");

        System.out.println(ANSI_YELLOW + "\n📦 Product Management:" + ANSI_RESET);
        System.out.println("  add-product <sellerId> <title> <desc> <price> <stock> [category]");
        System.out.println("                                     Add a new product");
        System.out.println("  update-product <productId> <title> <desc> <price> <stock> [category] [status]");
        System.out.println("                                     Update product details");
        System.out.println("  products                           List all products");
        System.out.println("  product <productId>                Get product details");
        System.out.println("  search <keyword>                   Search products by title");

        System.out.println(ANSI_YELLOW + "\n🛒 Cart Management:" + ANSI_RESET);
        System.out.println("  cart <userId>                      View user's cart");
        System.out.println("  cart-add <userId> <productId> <qty>");
        System.out.println("                                     Add item to cart");
        System.out.println("  cart-update <userId> <productId> <qty>");
        System.out.println("                                     Update cart item quantity");
        System.out.println("  cart-remove <userId> <productId>   Remove item from cart");

        System.out.println(ANSI_YELLOW + "\n📝 Order Management:" + ANSI_RESET);
        System.out.println("  checkout <userId>                  Create order from cart");
        System.out.println("  orders [buyerId]                   List orders (optionally by buyer)");
        System.out.println("  order <orderId>                    Get order details");
        System.out.println("  order-status <orderId> <status>    Update order status");
        System.out.println("    Status: PENDING, PAID, SHIPPED, COMPLETED, CANCELED, REFUNDED");

        System.out.println(ANSI_YELLOW + "\n💳 Payment:" + ANSI_RESET);
        System.out.println("  pay <orderId> <method> <payerId> <cardNumber>");
        System.out.println("                                     Pay for an order");
        System.out.println("    Method: DEBIT, CREDIT");
        System.out.println("  payments [orderId]                 List payments");
        System.out.println("  seed-balance <cardNumber> <amount> Add balance to card (demo)");

        System.out.println(ANSI_YELLOW + "\n⭐ Reviews:" + ANSI_RESET);
        System.out.println("  review <orderId> <productId> <buyerId> <rating:1-5> \"<comment>\"");
        System.out.println("                                     Add a product review");
        System.out.println("  reviews <productId>                List product reviews");
        System.out.println("  rating <productId>                 Get average product rating");

        System.out.println(ANSI_YELLOW + "\n💾 Data Management:" + ANSI_RESET);
        System.out.println("  export <filePath>                  Export all data to JSON file");
        System.out.println("  import <filePath>                  Import data from JSON file");

        System.out.println();
        printInfo("Tip: Use quotes for arguments with spaces, e.g., \"My Product Title\"");
        System.out.println();
    }

    // ==================== User Commands ====================

    private static void handleRegister(String[] parts) {
        if (parts.length < 3) {
            printUsage("register <email> <password> [name] [phone]");
            return;
        }
        String email = parts[1];
        String password = parts[2];
        String name = parts.length > 3 ? parts[3] : email.split("@")[0];
        String phone = parts.length > 4 ? parts[4] : null;

        Result<User> result = userService.register(email, phone, password, name);
        User user = result.getData();
        printSuccess("User registered successfully!");
        System.out.println("  ID:    " + user.getId());
        System.out.println("  Name:  " + user.getName());
        System.out.println("  Email: " + user.getEmail());
        if (phone != null) {
            System.out.println("  Phone: " + user.getPhone());
        }
    }

    private static void handleLogin(String[] parts) {
        if (parts.length < 3) {
            printUsage("login <email> <password>");
            return;
        }
        String email = parts[1];
        String password = parts[2];

        Result<User> result = userService.login(email, password);
        User user = result.getData();
        printSuccess("Login successful! Welcome, " + user.getName() + "!");
        System.out.println("  User ID: " + user.getId());
    }

    private static void handleListUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            printInfo("No users registered yet.");
            return;
        }

        printHeader("All Users (" + users.size() + ")");
        printTableHeader("ID", "Name", "Email", "Phone");
        for (User u : users) {
            System.out.printf("%-15s %-15s %-15s %-15s%n",
                    truncate(u.getId(), 14),
                    truncate(u.getName(), 14),
                    truncate(u.getEmail(), 14),
                    u.getPhone() != null ? truncate(u.getPhone(), 14) : "-");
        }
    }

    private static void handleGetUser(String[] parts) {
        if (parts.length < 2) {
            printUsage("user <userId>");
            return;
        }
        User user = userRepository.findById(parts[1])
                .orElseThrow(() -> new NotFoundException("User not found: " + parts[1]));

        printHeader("User Details");
        System.out.println("  ID:         " + user.getId());
        System.out.println("  Name:       " + user.getName());
        System.out.println("  Email:      " + user.getEmail());
        System.out.println("  Phone:      " + (user.getPhone() != null ? user.getPhone() : "N/A"));
        System.out.println("  Created:    " + DATE_FORMAT.format(user.getCreatedAt()));
    }

    private static void handleUpdateProfile(String[] parts) {
        if (parts.length < 3) {
            printUsage("update-profile <userId> <name> [phone]");
            return;
        }
        String userId = parts[1];
        String name = parts[2];
        String phone = parts.length > 3 ? parts[3] : null;

        Result<User> result = userService.updateProfile(userId, name, phone);
        User user = result.getData();
        printSuccess("Profile updated successfully!");
        System.out.println("  Name:  " + user.getName());
        System.out.println("  Phone: " + (user.getPhone() != null ? user.getPhone() : "N/A"));
    }

    // ==================== Product Commands ====================

    private static void handleAddProduct(String[] parts) {
        if (parts.length < 6) {
            printUsage("add-product <sellerId> <title> <description> <price> <stock> [category]");
            return;
        }
        String sellerId = parts[1];
        String title = parts[2];
        String description = parts[3];
        double price = Double.parseDouble(parts[4]);
        int stock = Integer.parseInt(parts[5]);
        String category = parts.length > 6 ? parts[6] : "general";

        Result<Product> result = productService.create(sellerId, title, description, price, stock, category);
        Product product = result.getData();
        printSuccess("Product created successfully!");
        System.out.println("  ID:       " + product.getId());
        System.out.println("  Title:    " + product.getTitle());
        System.out.println("  Price:    $" + String.format("%.2f", product.getPrice()));
        System.out.println("  Stock:    " + product.getStock());
        System.out.println("  Category: " + product.getCategory());
    }

    private static void handleUpdateProduct(String[] parts) {
        if (parts.length < 6) {
            printUsage("update-product <productId> <title> <description> <price> <stock> [category] [status]");
            return;
        }
        String productId = parts[1];
        String title = parts[2];
        String description = parts[3];
        double price = Double.parseDouble(parts[4]);
        int stock = Integer.parseInt(parts[5]);
        String category = parts.length > 6 ? parts[6] : "general";
        ProductStatus status = parts.length > 7 ? ProductStatus.valueOf(parts[7].toUpperCase()) : ProductStatus.ACTIVE;

        Result<Product> result = productService.update(productId, title, description, price, stock, category, status);
        Product product = result.getData();
        printSuccess("Product updated successfully!");
        System.out.println("  ID:       " + product.getId());
        System.out.println("  Title:    " + product.getTitle());
        System.out.println("  Price:    $" + String.format("%.2f", product.getPrice()));
        System.out.println("  Stock:    " + product.getStock());
        System.out.println("  Status:   " + product.getStatus());
    }

    private static void handleListProducts() {
        Result<List<Product>> result = productService.list();
        List<Product> products = result.getData();

        if (products.isEmpty()) {
            printInfo("No products available.");
            return;
        }

        printHeader("All Products (" + products.size() + ")");
        printTableHeader("ID", "Title", "Price", "Stock", "Status");
        for (Product p : products) {
            String stockDisplay = p.getStock() > 0 ? String.valueOf(p.getStock()) : ANSI_RED + "0" + ANSI_RESET;
            System.out.printf("%-15s %-15s $%-13.2f %-15s %-15s%n",
                    truncate(p.getId(), 14),
                    truncate(p.getTitle(), 14),
                    p.getPrice(),
                    stockDisplay,
                    p.getStatus());
        }
    }

    private static void handleGetProduct(String[] parts) {
        if (parts.length < 2) {
            printUsage("product <productId>");
            return;
        }
        Result<Product> result = productService.get(parts[1]);
        Product p = result.getData();

        printHeader("Product Details");
        System.out.println("  ID:          " + p.getId());
        System.out.println("  Title:       " + p.getTitle());
        System.out.println("  Description: " + p.getDescription());
        System.out.println("  Price:       $" + String.format("%.2f", p.getPrice()));
        System.out.println("  Stock:       " + p.getStock());
        System.out.println("  Category:    " + p.getCategory());
        System.out.println("  Status:      " + p.getStatus());
        System.out.println("  Seller:      " + p.getSellerId());
        System.out.println("  Created:     " + DATE_FORMAT.format(p.getCreatedAt()));

        // Show rating
        Result<Double> ratingResult = reviewService.averageRating(p.getId());
        double rating = ratingResult.getData();
        System.out.println("  Rating:      " + (rating > 0 ? String.format("%.1f ⭐", rating) : "No reviews yet"));
    }

    private static void handleSearchProducts(String[] parts) {
        if (parts.length < 2) {
            printUsage("search <keyword>");
            return;
        }
        String keyword = parts[1].toLowerCase();
        Result<List<Product>> result = productService.list();
        List<Product> matches = result.getData().stream()
                .filter(p -> p.getTitle().toLowerCase().contains(keyword) ||
                        p.getDescription().toLowerCase().contains(keyword) ||
                        p.getCategory().toLowerCase().contains(keyword))
                .toList();

        if (matches.isEmpty()) {
            printInfo("No products found matching '" + keyword + "'.");
            return;
        }

        printHeader("Search Results (" + matches.size() + ")");
        printTableHeader("ID", "Title", "Price", "Stock");
        for (Product p : matches) {
            System.out.printf("%-15s %-15s $%-13.2f %-15d%n",
                    truncate(p.getId(), 14),
                    truncate(p.getTitle(), 14),
                    p.getPrice(),
                    p.getStock());
        }
    }

    // ==================== Cart Commands ====================

    private static void handleViewCart(String[] parts) {
        if (parts.length < 2) {
            printUsage("cart <userId>");
            return;
        }
        String userId = parts[1];
        Result<List<CartItem>> result = cartService.view(userId);
        List<CartItem> items = result.getData();

        if (items.isEmpty()) {
            printInfo("Cart is empty for user: " + userId);
            return;
        }

        printHeader("Shopping Cart - " + userId);
        printTableHeader("Product ID", "Title", "Quantity", "Unit Price", "Subtotal");

        double total = 0;
        for (CartItem item : items) {
            double subtotal = item.getQuantity() * item.getPriceSnapshot();
            total += subtotal;
            System.out.printf("%-15s %-15s %-15d $%-13.2f $%-13.2f%n",
                    truncate(item.getProductId(), 14),
                    truncate(productService.get(item.getProductId()).getData().getTitle(), 14),
                    item.getQuantity(),
                    item.getPriceSnapshot(),
                    subtotal);
        }
        System.out.println("─".repeat(60));
        System.out.printf(ANSI_BOLD + "Total: $%.2f" + ANSI_RESET + "%n", total);
    }

    private static void handleCartAdd(String[] parts) {
        if (parts.length < 4) {
            printUsage("cart-add <userId> <productId> <quantity>");
            return;
        }
        String userId = parts[1];
        String productId = parts[2];
        int qty = Integer.parseInt(parts[3]);

        cartService.addItem(userId, productId, qty);
        printSuccess("Added " + qty + " item(s) to cart.");

        // Show cart summary
        Result<List<CartItem>> cartResult = cartService.view(userId);
        int totalItems = cartResult.getData().stream().mapToInt(CartItem::getQuantity).sum();
        System.out.println("  Cart now has " + totalItems + " item(s).");
    }

    private static void handleCartUpdate(String[] parts) {
        if (parts.length < 4) {
            printUsage("cart-update <userId> <productId> <quantity>");
            return;
        }
        String userId = parts[1];
        String productId = parts[2];
        int qty = Integer.parseInt(parts[3]);

        cartService.updateQuantity(userId, productId, qty);
        printSuccess("Cart updated. Quantity set to " + qty + ".");
    }

    private static void handleCartRemove(String[] parts) {
        if (parts.length < 3) {
            printUsage("cart-remove <userId> <productId>");
            return;
        }
        String userId = parts[1];
        String productId = parts[2];

        cartService.removeItem(userId, productId);
        printSuccess("Item removed from cart.");
    }

    // ==================== Order Commands ====================

    private static void handleOrderCreate(String[] parts) {
        if (parts.length < 2) {
            printUsage("checkout <userId>");
            return;
        }
        String userId = parts[1];
        Result<Order> result = orderService.createFromCart(userId);
        Order order = result.getData();

        printSuccess("Order created successfully!");
        System.out.println("  Order ID:  " + order.getId());
        System.out.println("  Amount:    $" + String.format("%.2f", order.getAmount()));
        System.out.println("  Status:    " + order.getStatus());
        System.out.println("  Items:     " + order.getItems().size());
        System.out.println();
        printInfo("Use 'pay " + order.getId() + " DEBIT <userId> <cardNumber>' to pay for this order.");
    }

    private static void handleListOrders(String[] parts) {
        List<Order> orders;
        String title;

        if (parts.length > 1) {
            String buyerId = parts[1];
            Result<List<Order>> result = orderService.listByBuyer(buyerId);
            orders = result.getData();
            title = "Orders for " + buyerId;
        } else {
            orders = orderRepository.findAll();
            title = "All Orders";
        }

        if (orders.isEmpty()) {
            printInfo("No orders found.");
            return;
        }

        printHeader(title + " (" + orders.size() + ")");
        printTableHeader("Order ID", "Buyer", "Amount", "Status", "Created", "Updated");
        for (Order o : orders) {
            String statusColor = switch (o.getStatus()) {
                case PENDING -> ANSI_YELLOW;
                case PAID, SHIPPED -> ANSI_CYAN;
                case COMPLETED -> ANSI_GREEN;
                case CANCELED, REFUNDED -> ANSI_RED;
            };
            System.out.printf("%-15s %-15s $%-13.2f %s%-15s%s %-15s%n %-15s%n",
                    truncate(o.getId(), 14),
                    truncate(o.getBuyerId(), 14),
                    o.getAmount(),
                    statusColor, o.getStatus(), ANSI_RESET,
                    DATE_FORMAT.format(o.getCreatedAt()),
                    DATE_FORMAT.format(o.getUpdatedAt()));
        }
    }

    private static void handleGetOrder(String[] parts) {
        if (parts.length < 2) {
            printUsage("order <orderId>");
            return;
        }
        Result<Order> result = orderService.get(parts[1]);
        Order o = result.getData();

        printHeader("Order Details");
        System.out.println("  Order ID:  " + o.getId());
        System.out.println("  Buyer:     " + o.getBuyerId());
        System.out.println("  Status:    " + o.getStatus());
        System.out.println("  Created:   " + DATE_FORMAT.format(o.getCreatedAt()));
        System.out.println("  Updated:   " + DATE_FORMAT.format(o.getUpdatedAt()));
        System.out.println();
        System.out.println("  Items:");
        for (OrderItem item : o.getItems()) {
            System.out.printf("    - Product: %-15s  Qty: %-5d  Price: $%.2f%n",
                    item.getProductId(), item.getQuantity(), item.getPriceSnapshot());
        }
        System.out.println();
        System.out.printf("  " + ANSI_BOLD + "Total Amount: $%.2f" + ANSI_RESET + "%n", o.getAmount());
    }

    private static void handleOrderStatus(String[] parts) {
        if (parts.length < 3) {
            printUsage("order-status <orderId> <status>");
            System.out.println("  Status options: PENDING, PAID, SHIPPED, COMPLETED, CANCELED, REFUNDED");
            return;
        }
        String orderId = parts[1];
        OrderStatus status = OrderStatus.valueOf(parts[2].toUpperCase());

        OrderStatus previousStatus = orderService.get(orderId).getData().getStatus();
        Result<Order> result = orderService.updateStatus(orderId, status);
        Order order = result.getData();
        printSuccess("Order status updated to " + order.getStatus());
        if ((status == OrderStatus.CANCELED || status == OrderStatus.REFUNDED)
                && previousStatus != status) {
            int restoredUnits = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
            printInfo("Inventory restored for " + restoredUnits + " item(s).");
        }
    }

    // ==================== Payment Commands ====================

    private static void handlePay(String[] parts) {
        if (parts.length < 5) {
            printUsage("pay <orderId> <method:DEBIT|CREDIT> <payerId> <cardNumber>");
            return;
        }
        String orderId = parts[1];
        PaymentMethod method = PaymentMethod.valueOf(parts[2].toUpperCase());
        String payerId = parts[3];
        String cardNumber = parts[4];

        Result<Payment> result = paymentService.pay(orderId, method, payerId, cardNumber);
        Payment payment = result.getData();

        printSuccess("Payment processed successfully!");
        System.out.println("  Payment ID:  " + payment.getId());
        System.out.println("  Order ID:    " + payment.getOrderId());
        System.out.println("  Amount:      $" + String.format("%.2f", payment.getAmount()));
        System.out.println("  Method:      " + payment.getMethod());
        System.out.println("  Status:      " + payment.getStatus());
    }

    private static void handleListPayments(String[] parts) {
        List<Payment> payments = paymentRepository.findAll();

        if (parts.length > 1) {
            String orderId = parts[1];
            payments = payments.stream()
                    .filter(p -> p.getOrderId().equals(orderId))
                    .toList();
        }

        if (payments.isEmpty()) {
            printInfo("No payments found.");
            return;
        }

        printHeader("Payments (" + payments.size() + ")");
        printTableHeader("Payment ID", "Order ID", "Amount", "Method", "Status");
        for (Payment p : payments) {
            System.out.printf("%-15s %-15s $%-13.2f %-15s %-15s%n",
                    truncate(p.getId(), 14),
                    truncate(p.getOrderId(), 14),
                    p.getAmount(),
                    p.getMethod(),
                    p.getStatus());
        }
    }

    private static void handleSeedBalance(String[] parts) {
        if (parts.length < 3) {
            printUsage("seed-balance <cardNumber> <amount>");
            return;
        }
        String cardNumber = parts[1];
        double amount = Double.parseDouble(parts[2]);

        bankService.seedBalance(cardNumber, amount);
        printSuccess("Added $" + String.format("%.2f", amount) + " to card " + cardNumber);
    }

    // ==================== Review Commands ====================

    private static void handleAddReview(String[] parts) {
        if (parts.length < 6) {
            printUsage("review <orderId> <productId> <buyerId> <rating:1-5> \"<comment>\"");
            return;
        }
        String orderId = parts[1];
        String productId = parts[2];
        String buyerId = parts[3];
        int rating = Integer.parseInt(parts[4]);
        String comment = parts[5];

        Result<Review> result = reviewService.addReview(orderId, productId, buyerId, rating, comment);
        Review review = result.getData();

        printSuccess("Review added successfully!");
        System.out.println("  Review ID: " + review.getId());
        System.out.println("  Rating:    " + "⭐".repeat(rating));
        System.out.println("  Comment:   " + review.getComment());
    }

    private static void handleListReviews(String[] parts) {
        if (parts.length < 2) {
            printUsage("reviews <productId>");
            return;
        }
        String productId = parts[1];
        Result<List<Review>> result = reviewService.list(productId);
        List<Review> reviews = result.getData();

        if (reviews.isEmpty()) {
            printInfo("No reviews yet for this product.");
            return;
        }

        Result<Double> avgResult = reviewService.averageRating(productId);
        printHeader("Reviews (" + reviews.size() + ") - Average: " +
                String.format("%.1f", avgResult.getData()) + " ⭐");

        for (Review r : reviews) {
            System.out.println();
            System.out.println("  " + "⭐".repeat(r.getRating()) + "☆".repeat(5 - r.getRating()) +
                    "  by " + r.getBuyerId());
            System.out.println("  \"" + r.getComment() + "\"");
            System.out.println("  " + ANSI_CYAN + "Order: " + r.getOrderId() + ANSI_RESET);
        }
    }

    private static void handleGetRating(String[] parts) {
        if (parts.length < 2) {
            printUsage("rating <productId>");
            return;
        }
        String productId = parts[1];
        Result<Double> result = reviewService.averageRating(productId);
        double rating = result.getData();

        if (rating == 0) {
            printInfo("No reviews yet for this product.");
        } else {
            System.out.println("Average Rating: " + String.format("%.1f", rating) + " ⭐");
        }
    }

    // ==================== Data Management Commands ====================

    private static void handleExport(String[] parts) {
        if (parts.length < 2) {
            printUsage("export <filePath>");
            return;
        }
        Path path = Paths.get(parts[1]);
        DataSnapshot snapshot = DataPersistenceService.exportAll(
                userRepository, productRepository, orderRepository, paymentRepository, reviewRepository);

        try {
            DataPersistenceService.writeToFile(snapshot, path);
            printSuccess("Data exported to " + path.toAbsolutePath());
            System.out.println("  Users:    " + snapshot.getUsers().size());
            System.out.println("  Products: " + snapshot.getProducts().size());
            System.out.println("  Orders:   " + snapshot.getOrders().size());
            System.out.println("  Payments: " + snapshot.getPayments().size());
            System.out.println("  Reviews:  " + snapshot.getReviews().size());
        } catch (Exception e) {
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }

    private static void handleImport(String[] parts) {
        if (parts.length < 2) {
            printUsage("import <filePath>");
            return;
        }
        Path path = Paths.get(parts[1]);

        try {
            DataSnapshot snapshot = DataPersistenceService.readFromFile(path);
            DataPersistenceService.importAll(snapshot,
                    userRepository, productRepository, orderRepository, paymentRepository, reviewRepository);
            printSuccess("Data imported from " + path.toAbsolutePath());
            System.out.println("  Users:    " + snapshot.getUsers().size());
            System.out.println("  Products: " + snapshot.getProducts().size());
            System.out.println("  Orders:   " + snapshot.getOrders().size());
            System.out.println("  Payments: " + snapshot.getPayments().size());
            System.out.println("  Reviews:  " + snapshot.getReviews().size());
        } catch (Exception e) {
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
    }

    private static void handleStats() {
        printHeader("Platform Statistics");

        int userCount = userRepository.findAll().size();
        List<Product> products = productRepository.findAll();
        List<Order> orders = orderRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        System.out.println("  👤 Users:     " + userCount);
        System.out.println("  📦 Products:  " + products.size());
        System.out.println("  📝 Orders:    " + orders.size());
        System.out.println("  💳 Payments:  " + payments.size());

        if (!orders.isEmpty()) {
            double totalSales = orders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.COMPLETED ||
                            o.getStatus() == OrderStatus.PAID ||
                            o.getStatus() == OrderStatus.SHIPPED)
                    .mapToDouble(Order::getAmount)
                    .sum();
            long pendingOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
            long completedOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();

            System.out.println();
            System.out.println("  💰 Total Sales:      $" + String.format("%.2f", totalSales));
            System.out.println("  ⏳ Pending Orders:   " + pendingOrders);
            System.out.println("  ✅ Completed Orders: " + completedOrders);
        }

        if (!products.isEmpty()) {
            int totalStock = products.stream().mapToInt(Product::getStock).sum();
            long outOfStock = products.stream().filter(p -> p.getStock() == 0).count();
            System.out.println();
            System.out.println("  📊 Total Stock:     " + totalStock + " items");
            if (outOfStock > 0) {
                System.out.println("  ⚠️  Out of Stock:   " + outOfStock + " products");
            }
        }
    }

    // ==================== Utilities ====================

    private static void printUsage(String usage) {
        printWarning("Usage: " + usage);
    }

    private static String truncate(String str, int maxLen) {
        if (str == null)
            return "";
        return str.length() > maxLen ? str.substring(0, maxLen - 2) + ".." : str;
    }
}
