package c2c.cart;

public class CartItem {
    private final String userId;
    private final String productId;
    private int quantity;
    private final double priceSnapshot;

    public CartItem(String userId, String productId, int quantity, double priceSnapshot) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.priceSnapshot = priceSnapshot;
    }

    public String getUserId() {
        return userId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
