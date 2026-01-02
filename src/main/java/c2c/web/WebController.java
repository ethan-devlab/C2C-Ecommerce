package c2c.web;

import c2c.cart.CartItem;
import c2c.cart.CartService;
import c2c.common.Result;
import c2c.order.Order;
import c2c.order.OrderRepository;
import c2c.order.OrderService;
import c2c.order.OrderStatus;
import c2c.payment.Payment;
import c2c.payment.PaymentMethod;
import c2c.payment.PaymentRepository;
import c2c.payment.PaymentService;
import c2c.product.Product;
import c2c.product.ProductRepository;
import c2c.product.ProductService;
import c2c.product.ProductStatus;
import c2c.review.ReviewService;
import c2c.user.User;
import c2c.user.UserRepository;
import c2c.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller powering the web GUI. Every CLI capability (except import/export
 * and seed-balance)
 * has a matching page or action here.
 */
@Controller
public class WebController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final ReviewService reviewService;

    public WebController(UserService userService,
            UserRepository userRepository,
            ProductService productService,
            ProductRepository productRepository,
            CartService cartService,
            OrderService orderService,
            OrderRepository orderRepository,
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            ReviewService reviewService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.productService = productService;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.reviewService = reviewService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpSession session) {
        Object currentUser = session.getAttribute("currentUser");
        if (!model.containsAttribute("currentUser")) {
            model.addAttribute("currentUser", currentUser);
        }
    }

    @ModelAttribute("orderStatuses")
    public OrderStatus[] orderStatuses() {
        return OrderStatus.values();
    }

    @ModelAttribute("productStatuses")
    public ProductStatus[] productStatuses() {
        return ProductStatus.values();
    }

    // ===== DASHBOARD =====
    @GetMapping("/")
    public String home(Model model) {
        List<User> users = userRepository.findAll();
        List<Product> products = productRepository.findAll();
        List<Order> orders = orderRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        model.addAttribute("userCount", users.size());
        model.addAttribute("productCount", products.size());
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("paymentCount", payments.size());
        model.addAttribute("pendingOrders", orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count());
        model.addAttribute("latestOrders", orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .limit(4)
                .toList());
        model.addAttribute("spotlightProducts", products.stream()
                .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
                .limit(4)
                .toList());
        return "index";
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        List<Product> products = productRepository.findAll();
        List<Order> orders = orderRepository.findAll();
        double totalSales = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID
                        || o.getStatus() == OrderStatus.SHIPPED
                        || o.getStatus() == OrderStatus.COMPLETED)
                .mapToDouble(Order::getAmount)
                .sum();

        model.addAttribute("userCount", userRepository.findAll().size());
        model.addAttribute("productCount", products.size());
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("paymentCount", paymentRepository.findAll().size());
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("pendingOrders", orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count());
        model.addAttribute("completedOrders",
                orders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count());
        model.addAttribute("totalStock", products.stream().mapToInt(Product::getStock).sum());
        model.addAttribute("outOfStock", products.stream().filter(p -> p.getStock() == 0).count());
        model.addAttribute("statusBreakdown", orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting())));
        return "stats";
    }

    @GetMapping("/help")
    public String help() {
        return "help";
    }

    // ===== AUTH =====
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            User user = userService.login(email, password).getData();
            session.setAttribute("currentUser", user);
            ra.addFlashAttribute("success", "Welcome back, " + user.getName() + "!");
            return "redirect:/";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.removeAttribute("currentUser");
        ra.addFlashAttribute("success", "You have been logged out.");
        return "redirect:/";
    }

    // ===== USERS =====
    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users";
    }

    @PostMapping("/users/register")
    public String registerUser(@RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam String password,
            @RequestParam String name,
            RedirectAttributes ra) {
        try {
            userService.register(email, phone, password, name);
            ra.addFlashAttribute("success", "User registered: " + name);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }

    @GetMapping("/users/{userId}")
    public String viewUser(@PathVariable String userId, Model model, RedirectAttributes ra) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found");
            return "redirect:/users";
        }
        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("userOrders", orderRepository.findAll().stream()
                .filter(o -> o.getBuyerId().equals(userId))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList());
        return "user-detail";
    }

    @PostMapping("/users/{userId}/update")
    public String updateProfile(@PathVariable String userId,
            @RequestParam String name,
            @RequestParam(required = false) String phone,
            RedirectAttributes ra) {
        try {
            userService.updateProfile(userId, name, phone);
            ra.addFlashAttribute("success", "Profile updated");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users/" + userId;
    }

    // ===== PRODUCTS =====
    @GetMapping("/products")
    public String listProducts(Model model, @RequestParam(required = false) String search) {
        List<Product> products = productService.list().getData();
        if (search != null && !search.isBlank()) {
            String keyword = search.toLowerCase();
            products = products.stream()
                    .filter(p -> p.getTitle().toLowerCase().contains(keyword)
                            || p.getDescription().toLowerCase().contains(keyword)
                            || p.getCategory().toLowerCase().contains(keyword))
                    .toList();
        }
        Map<String, Double> ratings = products.stream()
                .collect(Collectors.toMap(Product::getId,
                        p -> reviewService.averageRating(p.getId()).getData()));
        model.addAttribute("products", products);
        model.addAttribute("productRatings", ratings);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("search", search);
        return "products";
    }

    @PostMapping("/products/add")
    public String addProduct(@RequestParam String sellerId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam double price,
            @RequestParam int stock,
            @RequestParam String category,
            RedirectAttributes ra) {
        try {
            productService.create(sellerId, title, description, price, stock, category);
            ra.addFlashAttribute("success", "Product created: " + title);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products";
    }

    @GetMapping("/products/{productId}")
    public String viewProduct(@PathVariable String productId, Model model, RedirectAttributes ra) {
        try {
            Product product = productService.get(productId).getData();
            model.addAttribute("product", product);
            model.addAttribute("rating", reviewService.averageRating(productId).getData());
            model.addAttribute("reviews", reviewService.list(productId).getData());
            model.addAttribute("sellers", userRepository.findAll());
            return "product-detail";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/products";
        }
    }

    @PostMapping("/products/{productId}/update")
    public String updateProduct(@PathVariable String productId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam double price,
            @RequestParam int stock,
            @RequestParam String category,
            @RequestParam String status,
            RedirectAttributes ra) {
        try {
            productService.update(productId, title, description, price, stock, category,
                    ProductStatus.valueOf(status));
            ra.addFlashAttribute("success", "Product updated");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/" + productId;
    }

    // ===== CART =====
    @GetMapping("/cart")
    public String viewCart(Model model, @RequestParam(required = false) String userId) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("products", productService.list().getData());
        model.addAttribute("cartItems", null);
        model.addAttribute("cartTotal", 0d);
        model.addAttribute("selectedUser", userId);
        if (userId != null && !userId.isBlank()) {
            List<CartItem> items = cartService.view(userId).getData();
            double total = items.stream().mapToDouble(i -> i.getQuantity() * i.getPriceSnapshot()).sum();
            model.addAttribute("cartItems", items);
            model.addAttribute("cartTotal", total);
        }
        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam String userId,
            @RequestParam String productId,
            @RequestParam int quantity,
            RedirectAttributes ra) {
        try {
            cartService.addItem(userId, productId, quantity);
            ra.addFlashAttribute("success", "Added to cart");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart?userId=" + userId;
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam String userId,
            @RequestParam String productId,
            @RequestParam int quantity,
            RedirectAttributes ra) {
        try {
            cartService.updateQuantity(userId, productId, quantity);
            ra.addFlashAttribute("success", "Quantity updated");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart?userId=" + userId;
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam String userId,
            @RequestParam String productId,
            RedirectAttributes ra) {
        try {
            cartService.removeItem(userId, productId);
            ra.addFlashAttribute("success", "Removed from cart");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart?userId=" + userId;
    }

    // ===== ORDERS =====
    @GetMapping("/orders")
    public String listOrders(Model model, @RequestParam(required = false) String buyerId) {
        List<Order> orders;
        if (buyerId != null && !buyerId.isBlank()) {
            orders = orderService.listByBuyer(buyerId).getData();
            model.addAttribute("selectedBuyer", buyerId);
        } else {
            orders = orderRepository.findAll();
        }
        model.addAttribute("orders", orders);
        model.addAttribute("users", userRepository.findAll());
        return "orders";
    }

    @GetMapping("/orders/{orderId}")
    public String viewOrder(@PathVariable String orderId, Model model, RedirectAttributes ra) {
        try {
            Order order = orderService.get(orderId).getData();
            List<Payment> relatedPayments = paymentRepository.findAll().stream()
                    .filter(p -> p.getOrderId().equals(orderId))
                    .toList();
            model.addAttribute("order", order);
            model.addAttribute("payments", relatedPayments);
            return "order-detail";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/orders";
        }
    }

    @PostMapping("/orders/create")
    public String createOrder(@RequestParam String buyerId, RedirectAttributes ra) {
        try {
            Result<Order> result = orderService.createFromCart(buyerId);
            ra.addFlashAttribute("success", "Order created: " + result.getData().getId());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders?buyerId=" + buyerId;
    }

    @PostMapping("/orders/status")
    public String updateOrderStatus(@RequestParam String orderId,
            @RequestParam String status,
            RedirectAttributes ra) {
        try {
            OrderStatus targetStatus = OrderStatus.valueOf(status);
            OrderStatus previousStatus = orderService.get(orderId).getData().getStatus();
            orderService.updateStatus(orderId, targetStatus);
            boolean restored = (targetStatus == OrderStatus.CANCELED || targetStatus == OrderStatus.REFUNDED)
                    && previousStatus != targetStatus;
            String message = restored
                    ? "Order status updated to " + status + " and inventory restored."
                    : "Order status updated to " + status;
            ra.addFlashAttribute("success", message);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders";
    }

    // ===== PAYMENTS =====
    @GetMapping("/payments")
    public String listPayments(Model model,
            @RequestParam(required = false) String orderId) {
        List<Payment> payments = paymentRepository.findAll();
        if (orderId != null && !orderId.isBlank()) {
            payments = payments.stream()
                    .filter(p -> p.getOrderId().equals(orderId))
                    .toList();
            model.addAttribute("filterOrderId", orderId);
        }
        model.addAttribute("payments", payments);
        model.addAttribute("pendingOrders", orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .toList());
        model.addAttribute("users", userRepository.findAll());
        return "payments";
    }

    @PostMapping("/payments/pay")
    public String processPayment(@RequestParam String orderId,
            @RequestParam String method,
            @RequestParam String payerId,
            @RequestParam String cardNumber,
            RedirectAttributes ra) {
        try {
            paymentService.pay(orderId, PaymentMethod.valueOf(method), payerId, cardNumber);
            ra.addFlashAttribute("success", "Payment successful for order " + orderId);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payments";
    }

    // ===== REVIEWS =====
    @GetMapping("/reviews")
    public String listReviews(Model model, @RequestParam(required = false) String productId) {
        model.addAttribute("products", productService.list().getData());
        model.addAttribute("selectedProduct", productId);
        model.addAttribute("reviews", List.of());
        model.addAttribute("rating", null);
        if (productId != null && !productId.isBlank()) {
            model.addAttribute("reviews", reviewService.list(productId).getData());
            model.addAttribute("rating", reviewService.averageRating(productId).getData());
        }
        model.addAttribute("completedOrders", orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .toList());
        return "reviews";
    }

    @PostMapping("/reviews/add")
    public String addReview(@RequestParam String orderId,
            @RequestParam String productId,
            @RequestParam String buyerId,
            @RequestParam int rating,
            @RequestParam String comment,
            RedirectAttributes ra) {
        try {
            reviewService.addReview(orderId, productId, buyerId, rating, comment);
            ra.addFlashAttribute("success", "Review added");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reviews?productId=" + productId;
    }
}
