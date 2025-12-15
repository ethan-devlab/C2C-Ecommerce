package c2c.review;

import java.time.Instant;

public class Review {
    private final String id;
    private final String orderId;
    private final String productId;
    private final String buyerId;
    private final int rating;
    private final String comment;
    private final Instant createdAt;

    public Review(String id, String orderId, String productId, String buyerId, int rating, String comment,
            Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.buyerId = buyerId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
