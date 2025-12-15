package c2c.cart;

import java.util.List;

public interface CartRepository {
    List<CartItem> getCart(String userId);

    void putItem(CartItem item);

    void removeItem(String userId, String productId);

    void clear(String userId);
}
