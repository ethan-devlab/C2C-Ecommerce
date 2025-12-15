package c2c.cart;

import c2c.common.exceptions.ValidationException;
import c2c.product.DefaultProductService;
import c2c.product.InMemoryProductRepository;
import c2c.product.ProductRepository;
import c2c.product.ProductService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartServiceTest {

    @Test
    void addItemFailsWhenStockInsufficient() {
        ProductRepository productRepository = new InMemoryProductRepository();
        ProductService productService = new DefaultProductService(productRepository);
        var product = productService.create("seller", "Item", "", 5.0, 1, "cat").getData();

        CartService cartService = new DefaultCartService(new InMemoryCartRepository(), productRepository);
        assertThrows(ValidationException.class, () -> cartService.addItem("buyer", product.getId(), 2));
    }

    @Test
    void updateQuantityFailsWhenStockInsufficient() {
        ProductRepository productRepository = new InMemoryProductRepository();
        ProductService productService = new DefaultProductService(productRepository);
        var product = productService.create("seller", "Item", "", 5.0, 1, "cat").getData();

        CartService cartService = new DefaultCartService(new InMemoryCartRepository(), productRepository);
        cartService.addItem("buyer", product.getId(), 1);
        assertThrows(ValidationException.class, () -> cartService.updateQuantity("buyer", product.getId(), 2));
    }

}
