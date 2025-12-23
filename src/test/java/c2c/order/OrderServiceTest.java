package c2c.order;

import c2c.cart.CartService;
import c2c.cart.DefaultCartService;
import c2c.cart.InMemoryCartRepository;
import c2c.common.Result;
import c2c.common.exceptions.NotFoundException;
import c2c.common.exceptions.ValidationException;
import c2c.product.DefaultProductService;
import c2c.product.InMemoryProductRepository;
import c2c.product.Product;
import c2c.product.ProductRepository;
import c2c.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderService 測試")
class OrderServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private ProductRepository productRepository;
    private ProductService productService;
    private InMemoryCartRepository cartRepository;
    private CartService cartService;
    private OrderRepository orderRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        productService = new DefaultProductService(productRepository);
        cartRepository = new InMemoryCartRepository();
        cartService = new DefaultCartService(cartRepository, productRepository);
        orderRepository = new InMemoryOrderRepository();
        orderService = new DefaultOrderService(orderRepository, cartRepository, productRepository);
    }

    @Nested
    @DisplayName("CreateFromCart 功能測試")
    class CreateFromCartTests {

        @Test
        @DisplayName("OS-C-001: 創建成功 - 單一商品")
        void createOrderSuccess_singleItem() {
            // Given: 準備單一商品和購物車
            Product product = productService.create("seller", "Book", "desc", 20.0, 5, "cat").getData();
            cartService.addItem("buyer", product.getId(), 2);

            // When: 從購物車創建訂單
            Result<Order> result = orderService.createFromCart("buyer");

            // Then: 訂單創建成功，庫存減少，購物車清空
            assertTrue(result.isSuccess());
            Order order = result.getData();
            assertEquals(OrderStatus.PENDING, order.getStatus());
            assertEquals(1, order.getItems().size());
            assertEquals(2, order.getItems().get(0).getQuantity());
            assertEquals(40.0, order.getAmount(), 0.01);
            assertEquals(3, productRepository.findById(product.getId()).orElseThrow().getStock());
            assertTrue(cartRepository.getCart("buyer").isEmpty());
        }

        @Test
        @DisplayName("OS-C-002: 創建成功 - 多個商品")
        void createOrderSuccess_multipleItems() {
            // Given: 準備多個商品和購物車
            Product product1 = productService.create("seller1", "Book", "desc", 20.0, 10, "cat").getData();
            Product product2 = productService.create("seller2", "Pen", "desc", 5.0, 10, "cat").getData();
            Product product3 = productService.create("seller1", "Notebook", "desc", 15.0, 10, "cat").getData();

            cartService.addItem("buyer", product1.getId(), 2);
            cartService.addItem("buyer", product2.getId(), 3);
            cartService.addItem("buyer", product3.getId(), 1);

            // When: 從購物車創建訂單
            Result<Order> result = orderService.createFromCart("buyer");

            // Then: 訂單創建成功，所有商品庫存減少
            assertTrue(result.isSuccess());
            Order order = result.getData();
            assertEquals(3, order.getItems().size());
            assertEquals(70.0, order.getAmount(), 0.01); // 20*2 + 5*3 + 15*1
            assertEquals(8, productRepository.findById(product1.getId()).orElseThrow().getStock());
            assertEquals(7, productRepository.findById(product2.getId()).orElseThrow().getStock());
            assertEquals(9, productRepository.findById(product3.getId()).orElseThrow().getStock());
        }

        @Test
        @DisplayName("OS-C-003: 創建失敗 - 購物車為空")
        void createOrderFail_emptyCart() {
            // Given: 空購物車
            // When & Then: 創建訂單拋出異常
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> orderService.createFromCart("buyer"));
            assertEquals("cart is empty", exception.getMessage());
        }

        @Test
        @DisplayName("OS-C-004: 創建失敗 - 商品不存在")
        void createOrderFail_productNotFound() {
            // Given: 購物車中有無效的商品ID
            cartRepository.putItem(new c2c.cart.CartItem("buyer", "invalid-product-id", 1, 100.0));

            // When & Then: 創建訂單拋出異常
            assertThrows(NotFoundException.class,
                    () -> orderService.createFromCart("buyer"));
        }

        @Test
        @DisplayName("OS-C-005: 創建失敗 - 庫存不足")
        void createOrderFail_insufficientStock() {
            // Given: 先成功添加商品到購物車，然後減少庫存使其不足
            Product product = productService.create("seller", "Book", "desc", 20.0, 5, "cat").getData();
            cartService.addItem("buyer", product.getId(), 3);
            
            // 手動減少庫存至不足以滿足訂單
            product.setStock(2);
            productRepository.save(product);

            // When & Then: 創建訂單時因庫存不足拋出異常
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> orderService.createFromCart("buyer"));
            assertTrue(exception.getMessage().contains("insufficient stock"));
        }

        @Test
        @DisplayName("OS-C-006: 創建成功 - 購買全部庫存（邊界值）")
        void createOrderSuccess_buyAllStock() {
            // Given: 購買全部庫存
            Product product = productService.create("seller", "Book", "desc", 20.0, 5, "cat").getData();
            cartService.addItem("buyer", product.getId(), 5);

            // When: 從購物車創建訂單
            Result<Order> result = orderService.createFromCart("buyer");

            // Then: 訂單創建成功，庫存變為0
            assertTrue(result.isSuccess());
            assertEquals(0, productRepository.findById(product.getId()).orElseThrow().getStock());
        }
    }

    @Nested
    @DisplayName("UpdateStatus 功能測試")
    class UpdateStatusTests {

        private Order createTestOrder() {
            Product product = productService.create("seller", "Book", "desc", 20.0, 10, "cat").getData();
            cartService.addItem("buyer", product.getId(), 1);
            return orderService.createFromCart("buyer").getData();
        }

        @Test
        @DisplayName("OS-U-001: 狀態轉換成功 - PENDING→PAID")
        void updateStatusSuccess_pendingToPaid() {
            // Given: PENDING狀態的訂單
            Order order = createTestOrder();
            assertEquals(OrderStatus.PENDING, order.getStatus());

            // When: 更新為PAID
            Result<Order> result = orderService.updateStatus(order.getId(), OrderStatus.PAID);

            // Then: 更新成功
            assertTrue(result.isSuccess());
            assertEquals(OrderStatus.PAID, result.getData().getStatus());
        }

        @Test
        @DisplayName("OS-U-002: 狀態轉換成功 - PENDING→CANCELED")
        void updateStatusSuccess_pendingToCanceled() {
            // Given: PENDING狀態的訂單
            Order order = createTestOrder();

            // When: 更新為CANCELED
            Result<Order> result = orderService.updateStatus(order.getId(), OrderStatus.CANCELED);

            // Then: 更新成功
            assertTrue(result.isSuccess());
            assertEquals(OrderStatus.CANCELED, result.getData().getStatus());
        }

        @Test
        @DisplayName("OS-U-003: 狀態轉換失敗 - PENDING→COMPLETED（非法轉換）")
        void updateStatusFail_pendingToCompleted() {
            // Given: PENDING狀態的訂單
            Order order = createTestOrder();

            // When & Then: 非法狀態轉換拋出異常
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> orderService.updateStatus(order.getId(), OrderStatus.COMPLETED));
            assertEquals("invalid status transition", exception.getMessage());
        }

        @Test
        @DisplayName("OS-U-004: 更新失敗 - 訂單不存在")
        void updateStatusFail_orderNotFound() {
            // Given: 無效的訂單ID
            String invalidOrderId = "invalid-order-id";

            // When & Then: 拋出NotFoundException
            assertThrows(NotFoundException.class,
                    () -> orderService.updateStatus(invalidOrderId, OrderStatus.PAID));
        }

        @ParameterizedTest(name = "有效狀態轉換: {0} → {1}")
        @CsvSource({
                "PENDING, PAID",
                "PENDING, CANCELED",
                "PAID, SHIPPED",
                "PAID, CANCELED",
                "PAID, REFUNDED",
                "SHIPPED, COMPLETED",
                "SHIPPED, REFUNDED"
        })
        @DisplayName("有效狀態轉換測試")
        void validStatusTransitions(String fromStatus, String toStatus) {
            // Given: 創建訂單並設置初始狀態
            Order order = createTestOrder();
            OrderStatus from = OrderStatus.valueOf(fromStatus);
            OrderStatus to = OrderStatus.valueOf(toStatus);

            // 設置訂單到fromStatus
            if (from != OrderStatus.PENDING) {
                order.setStatus(from);
                orderRepository.save(order);
            }

            // When: 執行狀態轉換
            Result<Order> result = orderService.updateStatus(order.getId(), to);

            // Then: 轉換成功
            assertTrue(result.isSuccess());
            assertEquals(to, result.getData().getStatus());
        }

        @ParameterizedTest(name = "非法狀態轉換: {0} → {1}")
        @CsvSource({
                "PENDING, SHIPPED",
                "PENDING, COMPLETED",
                "PENDING, REFUNDED",
                "PAID, COMPLETED",
                "COMPLETED, PENDING",
                "COMPLETED, PAID",
                "COMPLETED, SHIPPED",
                "CANCELED, PENDING",
                "CANCELED, PAID"
        })
        @DisplayName("非法狀態轉換測試")
        void invalidStatusTransitions(String fromStatus, String toStatus) {
            // Given: 創建訂單並設置初始狀態
            Order order = createTestOrder();
            OrderStatus from = OrderStatus.valueOf(fromStatus);
            OrderStatus to = OrderStatus.valueOf(toStatus);

            order.setStatus(from);
            orderRepository.save(order);

            // When & Then: 非法轉換拋出異常
            assertThrows(ValidationException.class,
                    () -> orderService.updateStatus(order.getId(), to));
        }
    }

    @Nested
    @DisplayName("Get 功能測試")
    class GetOrderTests {

        @Test
        @DisplayName("查詢訂單成功")
        void getOrderSuccess() {
            // Given: 創建訂單
            Product product = productService.create("seller", "Book", "desc", 20.0, 10, "cat").getData();
            cartService.addItem("buyer", product.getId(), 1);
            Order order = orderService.createFromCart("buyer").getData();

            // When: 查詢訂單
            Result<Order> result = orderService.get(order.getId());

            // Then: 查詢成功
            assertTrue(result.isSuccess());
            assertEquals(order.getId(), result.getData().getId());
        }

        @Test
        @DisplayName("查詢訂單失敗 - 訂單不存在")
        void getOrderFail_notFound() {
            // Given: 無效的訂單ID
            // When & Then: 拋出NotFoundException
            assertThrows(NotFoundException.class,
                    () -> orderService.get("invalid-order-id"));
        }
    }

    @Nested
    @DisplayName("ListByBuyer 功能測試")
    class ListByBuyerTests {

        @Test
        @DisplayName("查詢買家訂單列表成功")
        void listByBuyerSuccess() {
            // Given: 創建多個訂單
            Product product = productService.create("seller", "Book", "desc", 20.0, 10, "cat").getData();

            cartService.addItem("buyer", product.getId(), 1);
            orderService.createFromCart("buyer");

            cartService.addItem("buyer", product.getId(), 2);
            orderService.createFromCart("buyer");

            // When: 查詢買家訂單
            Result<java.util.List<Order>> result = orderService.listByBuyer("buyer");

            // Then: 查詢成功
            assertTrue(result.isSuccess());
            assertEquals(2, result.getData().size());
        }

        @Test
        @DisplayName("查詢買家訂單列表 - 無訂單")
        void listByBuyerEmpty() {
            // Given: 買家無訂單
            // When: 查詢買家訂單
            Result<java.util.List<Order>> result = orderService.listByBuyer("buyer-without-orders");

            // Then: 返回空列表
            assertTrue(result.isSuccess());
            assertTrue(result.getData().isEmpty());
        }
    }
}
