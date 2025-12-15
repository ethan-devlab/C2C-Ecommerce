package c2c.bank;

import c2c.payment.PaymentMethod;

import java.time.Instant;

public class Transaction {
    private final String id;
    private final String payerId;
    private final double amount;
    private final PaymentMethod method;
    private final String lockKey;
    private TransactionStatus status;
    private final Instant startedAt;
    private Instant endedAt;
    private String message;

    public Transaction(String id, String payerId, double amount, PaymentMethod method, String lockKey,
            TransactionStatus status, Instant startedAt, Instant endedAt, String message) {
        this.id = id;
        this.payerId = payerId;
        this.amount = amount;
        this.method = method;
        this.lockKey = lockKey;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public String getPayerId() {
        return payerId;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getLockKey() {
        return lockKey;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
        this.endedAt = Instant.now();
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
