package c2c.order;

public class OrderItem {
    private final String productId;
    private final String titleSnapshot;
    private final double priceSnapshot;
    private final int quantity;
    private final String sellerId;

    public OrderItem(String productId, String titleSnapshot, double priceSnapshot, int quantity, String sellerId) {
        this.productId = productId;
        this.titleSnapshot = titleSnapshot;
        this.priceSnapshot = priceSnapshot;
        this.quantity = quantity;
        this.sellerId = sellerId;
    }

    public String getProductId() {
        return productId;
    }

    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    public double getPriceSnapshot() {
        return priceSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getSellerId() {
        return sellerId;
    }
}
