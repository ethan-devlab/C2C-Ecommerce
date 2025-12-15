package c2c.data;

import c2c.data.DataSnapshot.OrderItemSnapshot;
import c2c.data.DataSnapshot.OrderSnapshot;
import c2c.data.DataSnapshot.PaymentSnapshot;
import c2c.data.DataSnapshot.ProductSnapshot;
import c2c.data.DataSnapshot.ReviewSnapshot;
import c2c.data.DataSnapshot.UserSnapshot;
import c2c.order.OrderItem;
import c2c.user.User;
import c2c.user.UserRepository;
import c2c.user.UserStatus;
import c2c.product.Product;
import c2c.product.ProductRepository;
import c2c.product.ProductStatus;
import c2c.order.Order;
import c2c.order.OrderRepository;
import c2c.order.OrderStatus;
import c2c.payment.Payment;
import c2c.payment.PaymentMethod;
import c2c.payment.PaymentRepository;
import c2c.payment.PaymentStatus;
import c2c.review.Review;
import c2c.review.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

public final class DataPersistenceService {
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private DataPersistenceService() {
    }

    public static DataSnapshot exportAll(UserRepository users, ProductRepository products, OrderRepository orders,
            PaymentRepository payments, ReviewRepository reviews) {
        DataSnapshot snapshot = new DataSnapshot();
        snapshot.setUsers(
                users.findAll().stream().map(DataPersistenceService::toSnapshot).collect(Collectors.toList()));
        snapshot.setProducts(
                products.findAll().stream().map(DataPersistenceService::toSnapshot).collect(Collectors.toList()));
        snapshot.setOrders(
                orders.findAll().stream().map(DataPersistenceService::toSnapshot).collect(Collectors.toList()));
        snapshot.setPayments(
                payments.findAll().stream().map(DataPersistenceService::toSnapshot).collect(Collectors.toList()));
        snapshot.setReviews(
                reviews.findAll().stream().map(DataPersistenceService::toSnapshot).collect(Collectors.toList()));
        return snapshot;
    }

    public static void writeToFile(DataSnapshot snapshot, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), snapshot);
    }

    public static DataSnapshot readFromFile(Path path) throws IOException {
        return MAPPER.readValue(path.toFile(), DataSnapshot.class);
    }

    public static void importAll(DataSnapshot snapshot, UserRepository users, ProductRepository products,
            OrderRepository orders, PaymentRepository payments, ReviewRepository reviews) {
        if (snapshot.getUsers() != null) {
            snapshot.getUsers().forEach(s -> users.save(fromSnapshot(s)));
        }
        if (snapshot.getProducts() != null) {
            snapshot.getProducts().forEach(s -> products.save(fromSnapshot(s)));
        }
        if (snapshot.getOrders() != null) {
            snapshot.getOrders().forEach(s -> orders.save(fromSnapshot(s)));
        }
        if (snapshot.getPayments() != null) {
            snapshot.getPayments().forEach(s -> payments.save(fromSnapshot(s)));
        }
        if (snapshot.getReviews() != null) {
            snapshot.getReviews().forEach(s -> reviews.save(fromSnapshot(s)));
        }
    }

    private static UserSnapshot toSnapshot(User user) {
        UserSnapshot s = new UserSnapshot();
        s.id = user.getId();
        s.email = user.getEmail();
        s.phone = user.getPhone();
        s.hashedPassword = user.getHashedPassword();
        s.name = user.getName();
        s.roles = new ArrayList<>(user.getRoles());
        s.status = user.getStatus().name();
        s.createdAt = user.getCreatedAt();
        return s;
    }

    private static User fromSnapshot(UserSnapshot s) {
        return new User(s.id, s.email, s.phone, s.hashedPassword, s.name,
                s.roles == null ? Collections.emptySet() : Set.copyOf(s.roles), UserStatus.valueOf(s.status),
                s.createdAt);
    }

    private static ProductSnapshot toSnapshot(Product p) {
        ProductSnapshot s = new ProductSnapshot();
        s.id = p.getId();
        s.sellerId = p.getSellerId();
        s.title = p.getTitle();
        s.description = p.getDescription();
        s.price = p.getPrice();
        s.stock = p.getStock();
        s.category = p.getCategory();
        s.status = p.getStatus().name();
        s.createdAt = p.getCreatedAt();
        return s;
    }

    private static Product fromSnapshot(ProductSnapshot s) {
        return new Product(s.id, s.sellerId, s.title, s.description, s.price, s.stock, s.category,
                ProductStatus.valueOf(s.status), s.createdAt);
    }

    private static OrderSnapshot toSnapshot(Order order) {
        OrderSnapshot s = new OrderSnapshot();
        s.id = order.getId();
        s.buyerId = order.getBuyerId();
        s.amount = order.getAmount();
        s.status = order.getStatus().name();
        s.paymentId = order.getPaymentId();
        s.createdAt = order.getCreatedAt();
        s.updatedAt = order.getUpdatedAt();
        List<OrderItemSnapshot> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            OrderItemSnapshot is = new OrderItemSnapshot();
            is.productId = item.getProductId();
            is.titleSnapshot = item.getTitleSnapshot();
            is.priceSnapshot = item.getPriceSnapshot();
            is.quantity = item.getQuantity();
            is.sellerId = item.getSellerId();
            items.add(is);
        }
        s.items = items;
        return s;
    }

    private static Order fromSnapshot(OrderSnapshot s) {
        List<OrderItem> items = new ArrayList<>();
        if (s.items != null) {
            for (OrderItemSnapshot is : s.items) {
                items.add(new OrderItem(is.productId, is.titleSnapshot, is.priceSnapshot, is.quantity, is.sellerId));
            }
        }
        return new Order(s.id, s.buyerId, items, s.amount, OrderStatus.valueOf(s.status), s.paymentId, s.createdAt,
                s.updatedAt);
    }

    private static PaymentSnapshot toSnapshot(Payment p) {
        PaymentSnapshot s = new PaymentSnapshot();
        s.id = p.getId();
        s.orderId = p.getOrderId();
        s.method = p.getMethod().name();
        s.amount = p.getAmount();
        s.status = p.getStatus().name();
        s.transactionId = p.getTransactionId();
        s.createdAt = p.getCreatedAt();
        return s;
    }

    private static Payment fromSnapshot(PaymentSnapshot s) {
        Payment payment = new Payment(s.id, s.orderId, PaymentMethod.valueOf(s.method), s.amount,
                PaymentStatus.valueOf(s.status), s.transactionId, s.createdAt);
        payment.setStatus(PaymentStatus.valueOf(s.status));
        return payment;
    }

    private static ReviewSnapshot toSnapshot(Review r) {
        ReviewSnapshot s = new ReviewSnapshot();
        s.id = r.getId();
        s.orderId = r.getOrderId();
        s.productId = r.getProductId();
        s.buyerId = r.getBuyerId();
        s.rating = r.getRating();
        s.comment = r.getComment();
        s.createdAt = r.getCreatedAt();
        return s;
    }

    private static Review fromSnapshot(ReviewSnapshot s) {
        return new Review(s.id, s.orderId, s.productId, s.buyerId, s.rating, s.comment, s.createdAt);
    }
}
