package c2c.review;

import c2c.cart.DefaultCartService;
import c2c.cart.InMemoryCartRepository;
import c2c.common.Result;
import c2c.common.exceptions.ValidationException;
import c2c.order.DefaultOrderService;
import c2c.order.InMemoryOrderRepository;
import c2c.order.Order;
import c2c.order.OrderRepository;
import c2c.order.OrderService;
import c2c.order.OrderStatus;
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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReviewService 測試")
class ReviewServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private ProductRepository productRepository;
    private ProductService productService;
    private InMemoryCartRepository cartRepository;
    private DefaultCartService cartService;
    private OrderRepository orderRepository;
    private OrderService orderService;
    private ReviewRepository reviewRepository;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        productService = new DefaultProductService(productRepository);
        cartRepository = new InMemoryCartRepository();
        cartService = new DefaultCartService(cartRepository, productRepository);
        orderRepository = new InMemoryOrderRepository();
        orderService = new DefaultOrderService(orderRepository, cartRepository, productRepository);
        reviewRepository = new InMemoryReviewRepository();
        reviewService = new DefaultReviewService(reviewRepository, orderRepository);
    }

    /**
     * 輔助方法：創建已完成的訂單
     */
    private Order createCompletedOrder(String buyerId, String productId) {
        cartService.addItem(buyerId, productId, 1);
        Order order = orderService.createFromCart(buyerId).getData();
        orderService.updateStatus(order.getId(), OrderStatus.PAID);
        orderService.updateStatus(order.getId(), OrderStatus.SHIPPED);
        orderService.updateStatus(order.getId(), OrderStatus.COMPLETED);
        return orderRepository.findById(order.getId()).orElseThrow();
    }

    @Nested
    @DisplayName("AddReview 功能測試")
    class AddReviewTests {

        @Test
        @DisplayName("RS-A-001: 添加評論成功")
        void addReviewSuccess() {
            // Given: 已完成的訂單
            Product product = productService.create("seller", "Item", "desc", 10.0, 5, "cat").getData();
            Order order = createCompletedOrder("buyer", product.getId());

            // When: 添加評論
            Result<Review> result = reviewService.addReview(
                    order.getId(), product.getId(), "buyer", 5, "Great product!");

            // Then: 評論添加成功
            assertTrue(result.isSuccess());
            Review review = result.getData();
            assertNotNull(review.getId());
            assertEquals(order.getId(), review.getOrderId());
            assertEquals(product.getId(), review.getProductId());
            assertEquals("buyer", review.getBuyerId());
            assertEquals(5, review.getRating());
            assertEquals("Great product!", review.getComment());
        }

        @ParameterizedTest(name = "RS-A-002/003: rating為{0}時失敗（超出範圍）")
        @ValueSource(ints = { 0, 6, -1, 10 })
        @DisplayName("添加失敗 - rating超出範圍")
        void addReviewFail_ratingOutOfRange(int invalidRating) {
            // Given: 已完成的訂單
            Product product = productService.create("seller", "Item", "desc", 10.0, 5, "cat").getData();
            Order order = createCompletedOrder("buyer", product.getId());

            // When & Then: rating超出範圍拋出異常
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> reviewService.addReview(order.getId(), product.getId(), "buyer",
                            invalidRating, "comment"));
            assertEquals("rating must be 1-5", exception.getMessage());
        }

        @ParameterizedTest(name = "RS-A-004/009: 邊界值rating={0}成功")
        @CsvSource({
                "1, Bad",
                "5, Excellent"
        })
        @DisplayName("添加成功 - rating邊界值")
        void addReviewSuccess_boundaryRating(int rating, String comment) {
            // Given: 已完成的訂單
            Product product = productService.create("seller", "Item", "desc", 10.0, 5, "cat").getData();
            Order order = createCompletedOrder("buyer", product.getId());

            // When: 添加評論（邊界值）
            Result<Review> result = reviewService.addReview(
                    order.getId(), product.getId(), "buyer", rating, comment);

            // Then: 評論添加成功
            assertTrue(result.isSuccess());
            assertEquals(rating, result.getData().getRating());
        }

        @Test
        @DisplayName("RS-A-005: 添加失敗 - 訂單不存在")
        void addReviewFail_orderNotFound() {
            // Given: 無效的訂單ID
            String invalidOrderId = "invalid-order-id";

            // When & Then: 拋出ValidationException
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> reviewService.addReview(invalidOrderId, "product-id", "buyer", 5, "good"));
            assertEquals("order not found", exception.getMessage());
        }

        @Test
        @DisplayName("RS-A-006: 添加失敗 - buyerId不匹配")
        void addReviewFail_buyerMismatch() {
            // Given: 已完成的訂單，但使用不同的buyerId
            Product product = productService.create("seller", "Item", "desc", 10.0, 5, "cat").getData();
            Order order = createCompletedOrder("buyer1", product.getId());

            // When & Then: buyerId不匹配拋出異常
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> reviewService.addReview(order.getId(), product.getId(), "buyer2", 5, "good"));
            assertEquals("buyer mismatch", exception.getMessage());
        }

        @ParameterizedTest(name = "RS-A-007/008: 訂單狀態為{0}時添加失敗")
        @CsvSource({
                "PENDING",
                "PAID",
                "SHIPPED",
                "CANCELED",
                "REFUNDED"
        })
        @DisplayName("添加失敗 - 訂單未完成")
        void addReviewFail_orderNotCompleted(String orderStatus) {
            // Given: 非COMPLETED狀態的訂單
            Product product = productService.create("seller", "Item", "desc", 10.0, 5, "cat").getData();
            cartService.addItem("buyer", product.getId(), 1);
            Order order = orderService.createFromCart("buyer").getData();

            // 設置訂單狀態
            OrderStatus status = OrderStatus.valueOf(orderStatus);
            if (status != OrderStatus.PENDING) {
                order.setStatus(status);
                orderRepository.save(order);
            }

            // When & Then: 訂單未完成拋出異常
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> reviewService.addReview(order.getId(), product.getId(), "buyer", 5, "good"));
            assertEquals("order not completed", exception.getMessage());
        }

        @Test
        @DisplayName("同一訂單可以多次添加評論")
        void addMultipleReviewsForSameOrder() {
            // Given: 已完成的訂單
            Product product = productService.create("seller", "Item", "desc", 10.0, 5, "cat").getData();
            Order order = createCompletedOrder("buyer", product.getId());

            // When: 添加多個評論
            Review review1 = reviewService.addReview(
                    order.getId(), product.getId(), "buyer", 4, "ok").getData();
            Review review2 = reviewService.addReview(
                    order.getId(), product.getId(), "buyer", 5, "great").getData();

            // Then: 兩個評論都成功添加
            assertNotNull(review1.getId());
            assertNotNull(review2.getId());
            assertNotEquals(review1.getId(), review2.getId());
            assertEquals(2, reviewRepository.listByProduct(product.getId()).size());
        }
    }

    @Nested
    @DisplayName("List 功能測試")
    class ListReviewsTests {

        @Test
        @DisplayName("查詢商品評論列表成功")
        void listReviewsSuccess() {
            // Given: 創建多個評論
            Product product = productService.create("seller", "Item", "desc", 10.0, 5, "cat").getData();

            Order order1 = createCompletedOrder("buyer1", product.getId());
            Order order2 = createCompletedOrder("buyer2", product.getId());

            reviewService.addReview(order1.getId(), product.getId(), "buyer1", 5, "Excellent");
            reviewService.addReview(order2.getId(), product.getId(), "buyer2", 4, "Good");

            // When: 查詢評論列表
            Result<List<Review>> result = reviewService.list(product.getId());

            // Then: 查詢成功
            assertTrue(result.isSuccess());
            assertEquals(2, result.getData().size());
        }

        @Test
        @DisplayName("查詢商品評論列表 - 無評論")
        void listReviewsEmpty() {
            // Given: 商品無評論
            Product product = productService.create("seller", "Item", "desc", 10.0, 5, "cat").getData();

            // When: 查詢評論列表
            Result<List<Review>> result = reviewService.list(product.getId());

            // Then: 返回空列表
            assertTrue(result.isSuccess());
            assertTrue(result.getData().isEmpty());
        }
    }

    @Nested
    @DisplayName("AverageRating 功能測試")
    class AverageRatingTests {

        @Test
        @DisplayName("RS-R-001: 計算平均分 - 無評論")
        void averageRatingWithNoReviews() {
            // Given: 商品無評論
            Product product = productService.create("seller", "Item", "desc", 10.0, 5, "cat").getData();

            // When: 計算平均分
            Result<Double> result = reviewService.averageRating(product.getId());

            // Then: 返回0.0
            assertTrue(result.isSuccess());
            assertEquals(0.0, result.getData(), 0.001);
        }

        @Test
        @DisplayName("RS-R-002: 計算平均分 - 有多個評論")
        void averageRatingWithMultipleReviews() {
            // Given: 商品有多個評論
            Product product = productService.create("seller", "Item", "desc", 10.0, 5, "cat").getData();

            Order order1 = createCompletedOrder("buyer1", product.getId());
            Order order2 = createCompletedOrder("buyer2", product.getId());
            Order order3 = createCompletedOrder("buyer3", product.getId());

            reviewService.addReview(order1.getId(), product.getId(), "buyer1", 5, "Excellent");
            reviewService.addReview(order2.getId(), product.getId(), "buyer2", 4, "Good");
            reviewService.addReview(order3.getId(), product.getId(), "buyer3", 3, "Average");

            // When: 計算平均分
            Result<Double> result = reviewService.averageRating(product.getId());

            // Then: 返回正確的平均分 (5+4+3)/3 = 4.0
            assertTrue(result.isSuccess());
            assertEquals(4.0, result.getData(), 0.001);
        }

        @ParameterizedTest(name = "平均分測試: {0} = {1}")
        @CsvSource({
                "'5,5,5', 5.0",
                "'1,1,1', 1.0",
                "'5,4,3,2,1', 3.0",
                "'5,1', 3.0",
                "'4,4,5,5', 4.5"
        })
        @DisplayName("計算平均分 - 不同評分組合")
        void averageRatingVariousCombinations(String ratingsStr, double expectedAvg) {
            // Given: 商品有多個評論
            Product product = productService.create("seller", "Item", "desc", 10.0, 10, "cat").getData();
            String[] ratingArray = ratingsStr.split(",");

            for (int i = 0; i < ratingArray.length; i++) {
                String buyerId = "buyer" + i;
                Order order = createCompletedOrder(buyerId, product.getId());
                int rating = Integer.parseInt(ratingArray[i]);
                reviewService.addReview(order.getId(), product.getId(), buyerId, rating, "comment");
            }

            // When: 計算平均分
            Result<Double> result = reviewService.averageRating(product.getId());

            // Then: 返回正確的平均分
            assertTrue(result.isSuccess());
            assertEquals(expectedAvg, result.getData(), 0.001);
        }
    }

    @Nested
    @DisplayName("整合測試 - 完整評論流程")
    class IntegrationTests {

        @Test
        @DisplayName("完整評論流程：訂單創建→完成→評論→平均分計算")
        void completeReviewWorkflow() {
            // Given: 創建商品
            Product product = productService.create("seller", "Item", "desc", 10.0, 5, "cat").getData();

            // When: 完整流程
            // 1. 創建並完成訂單
            Order order = createCompletedOrder("buyer", product.getId());

            // 2. 添加評論
            Review review = reviewService.addReview(
                    order.getId(), product.getId(), "buyer", 4, "Good product").getData();

            // 3. 查詢評論列表
            List<Review> reviews = reviewService.list(product.getId()).getData();

            // 4. 計算平均分
            Double avgRating = reviewService.averageRating(product.getId()).getData();

            // Then: 所有操作成功
            assertNotNull(review.getId());
            assertEquals(1, reviews.size());
            assertEquals(4.0, avgRating, 0.001);
        }
    }
}
