package c2c.product;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {

    @Test
    void createAndUpdateProduct() {
        ProductRepository repo = new InMemoryProductRepository();
        ProductService service = new DefaultProductService(repo);

        Product product = service.create("seller1", "Item", "desc", 10.0, 5, "cat").getData();
        assertEquals(5, product.getStock());

        Product updated = service.update(product.getId(), "New", null, 12.5, 3, null, ProductStatus.ACTIVE).getData();
        assertEquals("New", updated.getTitle());
        assertEquals(3, updated.getStock());
        assertEquals(12.5, updated.getPrice());
    }
}
