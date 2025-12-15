package c2c.cart;

import c2c.common.Result;

import java.util.List;

public interface CartService {
    Result<List<CartItem>> addItem(String userId, String productId, int quantity);

    Result<List<CartItem>> updateQuantity(String userId, String productId, int quantity);

    Result<List<CartItem>> removeItem(String userId, String productId);

    Result<List<CartItem>> view(String userId);
}
