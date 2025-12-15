package c2c.cart;

import c2c.common.Result;
import c2c.common.exceptions.NotFoundException;
import c2c.common.exceptions.ValidationException;
import c2c.product.Product;
import c2c.product.ProductRepository;
import c2c.product.ProductStatus;

import java.util.List;

public class DefaultCartService implements CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public DefaultCartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Result<List<CartItem>> addItem(String userId, String productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("product not found"));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ValidationException("product inactive");
        }
        if (quantity <= 0) {
            throw new ValidationException("quantity must be positive");
        }
        if (product.getStock() < quantity) {
            throw new ValidationException("insufficient stock");
        }
        CartItem item = new CartItem(userId, productId, quantity, product.getPrice());
        cartRepository.putItem(item);
        return Result.ok(cartRepository.getCart(userId));
    }

    @Override
    public Result<List<CartItem>> updateQuantity(String userId, String productId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("quantity must be positive");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("product not found"));
        if (product.getStock() < quantity) {
            throw new ValidationException("insufficient stock");
        }
        CartItem item = new CartItem(userId, productId, quantity, product.getPrice());
        cartRepository.putItem(item);
        return Result.ok(cartRepository.getCart(userId));
    }

    @Override
    public Result<List<CartItem>> removeItem(String userId, String productId) {
        cartRepository.removeItem(userId, productId);
        return Result.ok(cartRepository.getCart(userId));
    }

    @Override
    public Result<List<CartItem>> view(String userId) {
        return Result.ok(cartRepository.getCart(userId));
    }
}
