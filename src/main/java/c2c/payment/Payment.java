package c2c.payment;

import java.time.Instant;

public class Payment {
    private final String id;
    private final String orderId;
    private final PaymentMethod method;
    private final double amount;
    private PaymentStatus status;
    private final String transactionId;
    private final Instant createdAt;

    public Payment(String id, String orderId, PaymentMethod method, double amount, PaymentStatus status,
            String transactionId, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.method = method;
        this.amount = amount;
        this.status = status;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}
