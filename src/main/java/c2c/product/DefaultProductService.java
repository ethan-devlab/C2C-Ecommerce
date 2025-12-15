package c2c.product;

import c2c.common.IdGenerator;
import c2c.common.Result;
import c2c.common.exceptions.NotFoundException;
import c2c.common.exceptions.ValidationException;

import java.time.Instant;
import java.util.List;

public class DefaultProductService implements ProductService {
    private final ProductRepository repository;

    public DefaultProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<Product> create(String sellerId, String title, String description, double price, int stock,
            String category) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("title is required");
        }
        if (price < 0 || stock < 0) {
            throw new ValidationException("price/stock must be positive");
        }
        Product product = new Product(IdGenerator.nextNumeric(), sellerId, title, description, price, stock, category,
                ProductStatus.ACTIVE, Instant.now());
        repository.save(product);
        return Result.ok(product);
    }

    @Override
    public Result<Product> update(String productId, String title, String description, double price, int stock,
            String category, ProductStatus status) {
        Product product = repository.findById(productId).orElseThrow(() -> new NotFoundException("product not found"));
        if (title != null) {
            product.setTitle(title);
        }
        if (description != null) {
            product.setDescription(description);
        }
        if (price >= 0) {
            product.setPrice(price);
        }
        if (stock >= 0) {
            product.setStock(stock);
        }
        if (category != null) {
            product.setCategory(category);
        }
        if (status != null) {
            product.setStatus(status);
        }
        repository.save(product);
        return Result.ok(product);
    }

    @Override
    public Result<List<Product>> list() {
        return Result.ok(repository.findAll());
    }

    @Override
    public Result<Product> get(String productId) {
        return Result.ok(repository.findById(productId).orElseThrow(() -> new NotFoundException("product not found")));
    }
}
