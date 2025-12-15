package c2c.review;

import c2c.cart.DefaultCartService;
import c2c.cart.InMemoryCartRepository;
import c2c.common.exceptions.ValidationException;
import c2c.order.DefaultOrderService;
import c2c.order.InMemoryOrderRepository;
import c2c.order.OrderRepository;
import c2c.order.OrderService;
import c2c.order.OrderStatus;
import c2c.product.DefaultProductService;
import c2c.product.InMemoryProductRepository;
import c2c.product.ProductRepository;
import c2c.product.ProductService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReviewServiceTest {

    @Test
    void addReviewRequiresCompletedOrder() {
        ProductRepository productRepository = new InMemoryProductRepository();
        ProductService productService = new DefaultProductService(productRepository);
        var product = productService.create("seller", "Item", "", 10.0, 1, "cat").getData();

        InMemoryCartRepository cartRepository = new InMemoryCartRepository();
        var cartService = new DefaultCartService(cartRepository, productRepository);
        cartService.addItem("buyer", product.getId(), 1);

        OrderRepository orderRepository = new InMemoryOrderRepository();
        OrderService orderService = new DefaultOrderService(orderRepository, cartRepository, productRepository);
        var order = orderService.createFromCart("buyer").getData();

        ReviewRepository reviewRepository = new InMemoryReviewRepository();
        ReviewService reviewService = new DefaultReviewService(reviewRepository, orderRepository);

        // Not completed yet
        assertThrows(ValidationException.class,
                () -> reviewService.addReview(order.getId(), product.getId(), "buyer", 5, "great"));

        // Transition to COMPLETED then succeed
        orderService.updateStatus(order.getId(), OrderStatus.PAID);
        orderService.updateStatus(order.getId(), OrderStatus.SHIPPED);
        orderService.updateStatus(order.getId(), OrderStatus.COMPLETED);
        Review review = reviewService.addReview(order.getId(), product.getId(), "buyer", 4, "ok").getData();
        assertEquals(1, reviewRepository.listByProduct(product.getId()).size());
        assertEquals(4, review.getRating());
        Review review2 = reviewService.addReview(order.getId(), product.getId(), "buyer", 5, "great").getData();
        assertEquals(2, reviewRepository.listByProduct(product.getId()).size());
        assertEquals(4.5, reviewService.averageRating(product.getId()).getData());
    }
}
