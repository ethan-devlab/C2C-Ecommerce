package c2c.product;

import c2c.common.Result;

import java.util.List;

public interface ProductService {
    Result<Product> create(String sellerId, String title, String description, double price, int stock, String category);

    Result<Product> update(String productId, String title, String description, double price, int stock, String category,
            ProductStatus status);

    Result<List<Product>> list();

    Result<Product> get(String productId);
}
