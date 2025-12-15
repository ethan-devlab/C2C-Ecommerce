package c2c.cart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryCartRepository implements CartRepository {
    private final Map<String, Map<String, CartItem>> carts = new HashMap<>();

    @Override
    public List<CartItem> getCart(String userId) {
        return new ArrayList<>(carts.getOrDefault(userId, Map.of()).values());
    }

    @Override
    public void putItem(CartItem item) {
        carts.computeIfAbsent(item.getUserId(), k -> new HashMap<>())
                .put(item.getProductId(), item);
    }

    @Override
    public void removeItem(String userId, String productId) {
        Map<String, CartItem> map = carts.get(userId);
        if (map != null) {
            map.remove(productId);
        }
    }

    @Override
    public void clear(String userId) {
        carts.remove(userId);
    }
}
