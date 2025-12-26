package c2c.cart;

import c2c.common.exceptions.NotFoundException;
import c2c.common.exceptions.ValidationException;
import c2c.product.DefaultProductService;
import c2c.product.InMemoryProductRepository;
import c2c.product.Product;
import c2c.product.ProductRepository;
import c2c.product.ProductService;
import c2c.product.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CartService 測試")
class CartServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private ProductRepository productRepository;
    private ProductService productService;
    private CartRepository cartRepository;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        productService = new DefaultProductService(productRepository);
        cartRepository = new InMemoryCartRepository();
        cartService = new DefaultCartService(cartRepository, productRepository);
    }

    // ==========================================
    // 3.1 AddItem 功能測試 (CS-A-001 ~ CS-A-007)
    // ==========================================
    
    @Nested
    @DisplayName("AddItem 功能測試")
    class AddItemTests {

        private Product testProduct;

        @BeforeEach
        void setUpProduct() {
            testProduct = productService.create("seller", "TestProduct", "desc", 50.0, 10, "cat").getData();
        }

        @Test
        @DisplayName("CS-A-001: 添加成功 - 商品活躍且庫存足夠")
        void addItem_success_active_sufficient_stock() {
            List<CartItem> cartItems = cartService.addItem("buyer", testProduct.getId(), 5).getData();

            assertEquals(1, cartItems.size());
            CartItem item = cartItems.get(0);
            assertEquals("buyer", item.getUserId());
            assertEquals(testProduct.getId(), item.getProductId());
            assertEquals(5, item.getQuantity());
            assertEquals(50.0, item.getPriceSnapshot(), 0.01);

            List<CartItem> viewItems = cartService.view("buyer").getData();
            assertEquals(1, viewItems.size());
            viewItems.get(0).setQuantity(6);
            assertEquals(6, viewItems.get(0).getQuantity());
        }

        @Test
        @DisplayName("CS-A-002: 添加失敗 - 商品不存在")
        void addItem_fail_product_not_found() {
            NotFoundException exception = assertThrows(NotFoundException.class, () ->
                    cartService.addItem("buyer", "invalid-product-id", 1)
            );
            assertEquals("product not found", exception.getMessage());
        }

        @Test
        @DisplayName("CS-A-003: 添加失敗 - 商品未啟用")
        void addItem_fail_product_inactive() {
            Product inactiveProduct = productService.create("seller", "Inactive", "desc", 50.0, 10, "cat").getData();
            productService.update(inactiveProduct.getId(), null, null, -1, -1, null, ProductStatus.INACTIVE);

            ValidationException exception = assertThrows(ValidationException.class, () ->
                    cartService.addItem("buyer", inactiveProduct.getId(), 1)
            );
            assertEquals("product inactive", exception.getMessage());
        }

        @Test
        @DisplayName("CS-A-004: 添加失敗 - 數量為0")
        void addItem_fail_quantity_zero() {
            ValidationException exception = assertThrows(ValidationException.class, () ->
                    cartService.addItem("buyer", testProduct.getId(), 0)
            );
            assertEquals("quantity must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("CS-A-005: 添加失敗 - 數量為負數")
        void addItem_fail_quantity_negative() {
            ValidationException exception = assertThrows(ValidationException.class, () ->
                    cartService.addItem("buyer", testProduct.getId(), -1)
            );
            assertEquals("quantity must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("CS-A-006: 添加失敗 - 庫存不足")
        void addItem_fail_insufficient_stock() {
            ValidationException exception = assertThrows(ValidationException.class, () ->
                    cartService.addItem("buyer", testProduct.getId(), 15)
            );
            assertEquals("insufficient stock", exception.getMessage());
        }

        @Test
        @DisplayName("CS-A-007: 添加成功 - 數量等於庫存(邊界值)")
        void addItem_success_quantity_equals_stock() {
            List<CartItem> cartItems = cartService.addItem("buyer", testProduct.getId(), 10).getData();

            assertEquals(1, cartItems.size());
            assertEquals(10, cartItems.get(0).getQuantity());

            List<CartItem> afterRemove = cartService.removeItem("buyer", testProduct.getId()).getData();
            assertTrue(afterRemove.isEmpty());
        }
    }

    // ==========================================
    // 3.2 UpdateQuantity 功能測試 (CS-U-001 ~ CS-U-004)
    // ==========================================
    
    @Nested
    @DisplayName("UpdateQuantity 功能測試")
    class UpdateQuantityTests {

        private Product testProduct;

        @BeforeEach
        void setUpProduct() {
            testProduct = productService.create("seller", "TestProduct", "desc", 50.0, 10, "cat").getData();
            cartService.addItem("buyer", testProduct.getId(), 5);
        }

        @Test
        @DisplayName("CS-U-001: 更新成功 - 新數量有效")
        void updateQuantity_success_valid_quantity() {
            List<CartItem> cartItems = cartService.updateQuantity("buyer", testProduct.getId(), 8).getData();

            assertEquals(1, cartItems.size());
            assertEquals(8, cartItems.get(0).getQuantity());

            List<CartItem> viewItems = cartService.view("buyer").getData();
            assertEquals(1, viewItems.size());
            assertEquals(8, viewItems.get(0).getQuantity());
        }

        @Test
        @DisplayName("CS-U-002: 更新失敗 - 數量為0")
        void updateQuantity_fail_quantity_zero() {
            ValidationException exception = assertThrows(ValidationException.class, () ->
                    cartService.updateQuantity("buyer", testProduct.getId(), 0)
            );
            assertEquals("quantity must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("CS-U-003: 更新失敗 - 數量為負數")
        void updateQuantity_fail_quantity_negative() {
            ValidationException exception = assertThrows(ValidationException.class, () ->
                    cartService.updateQuantity("buyer", testProduct.getId(), -1)
            );
            assertEquals("quantity must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("CS-U-003: 更新失敗 - 庫存不足")
        void updateQuantity_fail_insufficient_stock() {
            ValidationException exception = assertThrows(ValidationException.class, () ->
                    cartService.updateQuantity("buyer", testProduct.getId(), 15)
            );
            assertEquals("insufficient stock", exception.getMessage());
        }

        @Test
        @DisplayName("CS-U-004: 更新失敗 - 商品不存在")
        void updateQuantity_fail_product_not_found() {
            NotFoundException exception = assertThrows(NotFoundException.class, () ->
                    cartService.updateQuantity("buyer", "invalid-product-id", 5)
            );
            assertEquals("product not found", exception.getMessage());
            
            // 額外覆蓋：添加第二個商品並測試removeItem和view多商品場景
            Product product2 = productService.create("seller", "Product2", "desc", 30.0, 5, "cat").getData();
            cartService.addItem("buyer", product2.getId(), 2);
            
            // 驗證購物車有2個商品
            List<CartItem> twoItems = cartService.view("buyer").getData();
            assertEquals(2, twoItems.size());
            
            // 更新第一個商品
            List<CartItem> afterUpdate = cartService.updateQuantity("buyer", testProduct.getId(), 3).getData();
            assertEquals(2, afterUpdate.size());
            
            // 移除第一個商品
            List<CartItem> afterRemoveOne = cartService.removeItem("buyer", testProduct.getId()).getData();
            assertEquals(1, afterRemoveOne.size());
            assertEquals(product2.getId(), afterRemoveOne.get(0).getProductId());
            
            // 移除第二個商品
            List<CartItem> afterRemoveAll = cartService.removeItem("buyer", product2.getId()).getData();
            assertTrue(afterRemoveAll.isEmpty());
            
            // 驗證空購物車
            List<CartItem> emptyCart = cartService.view("buyer").getData();
            assertTrue(emptyCart.isEmpty());
            
            // 測試移除不存在的商品
            List<CartItem> removeNonExistent = cartService.removeItem("buyer", "nonexistent-id").getData();
            assertTrue(removeNonExistent.isEmpty());
        }
    }
}
