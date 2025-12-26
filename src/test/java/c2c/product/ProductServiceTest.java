package c2c.product;

import c2c.common.exceptions.NotFoundException;
import c2c.common.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductService 測試")
class ProductServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private ProductRepository repository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
        service = new DefaultProductService(repository);
    }

    // ==========================================
    // 2.1 Create 功能測試 (PS-C-001 ~ PS-C-007)
    // ==========================================

    @Nested
    @DisplayName("Create 功能測試")
    class CreateTests {

        @Test
        @DisplayName("PS-C-001: 創建成功 - 所有參數有效")
        void create_success_all_valid() {
            Product product = service.create("seller", "Product", "desc", 100.0, 10, "cat").getData();

            assertNotNull(product);
            assertNotNull(product.getId());
            assertEquals("Product", product.getTitle());
            assertEquals("desc", product.getDescription());
            assertEquals(100.0, product.getPrice(), 0.01);
            assertEquals(10, product.getStock());
            assertEquals("cat", product.getCategory());
            assertEquals(ProductStatus.ACTIVE, product.getStatus());
            assertEquals("seller", product.getSellerId());
            assertNotNull(product.getCreatedAt(), "createdAt should not be null");

            assertTrue(service.list().getData().stream().anyMatch(p -> p.getId().equals(product.getId())));
            assertEquals(product.getId(), service.get(product.getId()).getData().getId());
        }

        @Test
        @DisplayName("PS-C-002: 創建失敗 - title為null")
        void create_fail_title_null() {
            assertThrows(ValidationException.class, () ->
                            service.create("seller", null, "desc", 100.0, 10, "cat"),
                    "title is required"
            );
        }

        @Test
        @DisplayName("PS-C-003: 創建失敗 - title為空字串")
        void create_fail_title_empty() {
            assertThrows(ValidationException.class, () ->
                            service.create("seller", "", "desc", 100.0, 10, "cat"),
                    "title is required"
            );
            List<Product> products = service.list().getData();
            assertTrue(products.isEmpty());
        }

        @Test
        @DisplayName("PS-C-004: 創建失敗 - title為空白")
        void create_fail_title_blank() {
            assertThrows(ValidationException.class, () ->
                            service.create("seller", "   ", "desc", 100.0, 10, "cat"),
                    "title is required"
            );
            List<Product> products = service.list().getData();
            assertTrue(products.isEmpty());
        }

        @Test
        @DisplayName("PS-C-005: 創建失敗 - price為負整數")
        void create_fail_negative_price_integer() {
            assertThrows(ValidationException.class, () ->
                            service.create("seller", "Product", "desc", -1.0, 10, "cat"),
                    "price/stock must be positive"
            );
            assertTrue(service.list().getData().isEmpty());
        }

        @Test
        @DisplayName("PS-C-005: 創建失敗 - price為負小數")
        void create_fail_negative_price_decimal() {
            assertThrows(ValidationException.class, () ->
                            service.create("seller", "Product", "desc", -0.5, 10, "cat"),
                    "price/stock must be positive"
            );
            assertTrue(service.list().getData().isEmpty());
        }

        @Test
        @DisplayName("PS-C-005: 創建失敗 - stock為負數")
        void create_fail_negative_stock() {
            assertThrows(ValidationException.class, () ->
                            service.create("seller", "Product", "desc", 100.0, -1, "cat"),
                    "price/stock must be positive"
            );
            List<Product> products = service.list().getData();
            assertFalse(products.stream().anyMatch(p -> p.getStock() == -1));
        }

        @Test
        @DisplayName("PS-C-006: 創建成功 - price為0（邊界值）")
        void create_success_price_zero() {
            Product product = service.create("seller", "Product", "desc", 0.0, 10, "cat").getData();
            assertNotNull(product);
            assertEquals(0.0, product.getPrice(), 0.01);
            assertEquals(10, product.getStock());
            List<Product> allProducts = service.list().getData();
            assertTrue(allProducts.stream().anyMatch(p -> p.getId().equals(product.getId())));
        }

        @Test
        @DisplayName("PS-C-007: 創建成功 - stock為0（邊界值）")
        void create_success_stock_zero() {
            Product product = service.create("seller", "Product", "desc", 100.0, 0, "cat").getData();
            assertNotNull(product);
            assertEquals(100.0, product.getPrice(), 0.01);
            assertEquals(0, product.getStock());
            List<Product> allProducts = service.list().getData();
            assertTrue(allProducts.stream().anyMatch(p -> p.getId().equals(product.getId())));
        }
    }

    // ==========================================
    // 2.2 Update 功能測試 (PS-U-001 ~ PS-U-005)
    // ==========================================

    @Nested
    @DisplayName("Update 功能測試")
    class UpdateTests {

        private Product testProduct;

        @BeforeEach
        void setUpProduct() {
            testProduct = service.create("seller", "Original", "orig desc", 100.0, 10, "cat").getData();
        }

        @Test
        @DisplayName("PS-U-001: 更新成功 - 更新所有欄位")
        void update_success_all_fields() {
            // When
            Product updated = service.update(
                    testProduct.getId(),
                    "New Title",
                    "New Desc",
                    200.0,
                    20,
                    "newcat",
                    ProductStatus.INACTIVE
            ).getData();

            // Then
            assertEquals("New Title", updated.getTitle());
            assertEquals("New Desc", updated.getDescription());
            assertEquals(200.0, updated.getPrice(), 0.01);
            assertEquals(20, updated.getStock());
            assertEquals("newcat", updated.getCategory());
            assertEquals(ProductStatus.INACTIVE, updated.getStatus());

            Product retrieved = service.get(testProduct.getId()).getData();
            assertEquals("New Title", retrieved.getTitle());
            assertEquals(ProductStatus.INACTIVE, retrieved.getStatus());
        }

        @Test
        @DisplayName("PS-U-002: 更新成功 - 只更新title")
        void update_success_only_title() {
            Product original = testProduct;

            Product updated = service.update(
                    testProduct.getId(),
                    "New Title",
                    null,
                    -1,
                    -1,
                    null,
                    null
            ).getData();

            assertEquals("New Title", updated.getTitle());
            assertEquals("orig desc", updated.getDescription());
            assertEquals(100.0, updated.getPrice(), 0.01);
            assertEquals(10, updated.getStock());
            assertEquals("cat", updated.getCategory());

            assertTrue(original.equals(updated), "Product with same ID should be equal");
            assertTrue(updated.equals(original), "equals should be symmetric");
            assertTrue(updated.equals(updated), "equals should be reflexive");
            assertFalse(updated.equals(null), "equals should return false for null");
            assertFalse(updated.equals("string"), "equals should return false for different types");

            assertEquals(original.hashCode(), updated.hashCode(), "Equal objects should have same hashCode");
        }

        @Test
        @DisplayName("PS-U-003: 更新成功 - 只更新price和stock")
        void update_success_only_price_stock() {
            // When
            Product updated = service.update(
                    testProduct.getId(),
                    null,
                    null,
                    150.0,
                    15,
                    null,
                    null
            ).getData();

            assertEquals("Original", updated.getTitle());
            assertEquals(150.0, updated.getPrice(), 0.01);
            assertEquals(15, updated.getStock());

            List<Product> allProducts = service.list().getData();
            assertTrue(allProducts.size() >= 1);
            Product fromGet = service.get(testProduct.getId()).getData();
            assertEquals(150.0, fromGet.getPrice(), 0.01);
        }

        @Test
        @DisplayName("PS-U-004: 更新失敗 - 商品不存在")
        void update_fail_product_not_found() {
            NotFoundException exception = assertThrows(NotFoundException.class, () ->
                    service.update("invalid-id", "New Title", null, -1, -1, null, null)
            );
            assertEquals("product not found", exception.getMessage());
        }

        @Test
        @DisplayName("PS-U-005: 更新成功 - 只更新status")
        void update_success_only_status() {
            // When
            Product updated = service.update(
                    testProduct.getId(),
                    null,
                    null,
                    -1,
                    -1,
                    null,
                    ProductStatus.INACTIVE
            ).getData();

            assertEquals(ProductStatus.INACTIVE, updated.getStatus());
            assertEquals("Original", updated.getTitle());
            assertEquals(100.0, updated.getPrice(), 0.01);

            Product retrieved = service.get(testProduct.getId()).getData();
            assertEquals(ProductStatus.INACTIVE, retrieved.getStatus());

            List<Product> allProducts = service.list().getData();
            assertTrue(allProducts.stream().anyMatch(p ->
                    p.getId().equals(testProduct.getId()) && p.getStatus() == ProductStatus.INACTIVE
            ));
        }
    }
}
