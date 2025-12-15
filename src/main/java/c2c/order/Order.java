package c2c.order;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class Order {
    private final String id;
    private final String buyerId;
    private final List<OrderItem> items;
    private double amount;
    private OrderStatus status;
    private String paymentId;
    private final Instant createdAt;
    private Instant updatedAt;

    public Order(String id, String buyerId, List<OrderItem> items, double amount, OrderStatus status, String paymentId,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.buyerId = buyerId;
        this.items = items;
        this.amount = amount;
        this.status = status;
        this.paymentId = paymentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public double getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
        this.updatedAt = Instant.now();
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
