package c2c.data;

import java.time.Instant;
import java.util.List;

public class DataSnapshot {
    private List<UserSnapshot> users;
    private List<ProductSnapshot> products;
    private List<OrderSnapshot> orders;
    private List<PaymentSnapshot> payments;
    private List<ReviewSnapshot> reviews;

    public List<UserSnapshot> getUsers() {
        return users;
    }

    public void setUsers(List<UserSnapshot> users) {
        this.users = users;
    }

    public List<ProductSnapshot> getProducts() {
        return products;
    }

    public void setProducts(List<ProductSnapshot> products) {
        this.products = products;
    }

    public List<OrderSnapshot> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderSnapshot> orders) {
        this.orders = orders;
    }

    public List<PaymentSnapshot> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentSnapshot> payments) {
        this.payments = payments;
    }

    public List<ReviewSnapshot> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReviewSnapshot> reviews) {
        this.reviews = reviews;
    }

    public static class UserSnapshot {
        public String id;
        public String email;
        public String phone;
        public String hashedPassword;
        public String name;
        public List<String> roles;
        public String status;
        public Instant createdAt;
    }

    public static class ProductSnapshot {
        public String id;
        public String sellerId;
        public String title;
        public String description;
        public double price;
        public int stock;
        public String category;
        public String status;
        public Instant createdAt;
    }

    public static class OrderItemSnapshot {
        public String productId;
        public String titleSnapshot;
        public double priceSnapshot;
        public int quantity;
        public String sellerId;
    }

    public static class OrderSnapshot {
        public String id;
        public String buyerId;
        public List<OrderItemSnapshot> items;
        public double amount;
        public String status;
        public String paymentId;
        public Instant createdAt;
        public Instant updatedAt;
    }

    public static class PaymentSnapshot {
        public String id;
        public String orderId;
        public String method;
        public double amount;
        public String status;
        public String transactionId;
        public Instant createdAt;
    }

    public static class ReviewSnapshot {
        public String id;
        public String orderId;
        public String productId;
        public String buyerId;
        public int rating;
        public String comment;
        public Instant createdAt;
    }
}
