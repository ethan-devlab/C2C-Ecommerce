package c2c.order;

import c2c.cart.CartService;
import c2c.cart.DefaultCartService;
import c2c.cart.InMemoryCartRepository;
import c2c.common.exceptions.ValidationException;
import c2c.product.DefaultProductService;
import c2c.product.InMemoryProductRepository;
import c2c.product.ProductRepository;
import c2c.product.ProductService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    @Test
    void createOrderFromCartReducesStock() {
        ProductRepository productRepository = new InMemoryProductRepository();
        ProductService productService = new DefaultProductService(productRepository);
        var product = productService.create("seller", "Book", "desc", 20.0, 2, "cat").getData();

        InMemoryCartRepository cartRepository = new InMemoryCartRepository();
        CartService cartService = new DefaultCartService(cartRepository, productRepository);
        cartService.addItem("buyer", product.getId(), 1);

        OrderRepository orderRepository = new InMemoryOrderRepository();
        OrderService orderService = new DefaultOrderService(orderRepository, cartRepository, productRepository);

        Order order = orderService.createFromCart("buyer").getData();
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(1, order.getItems().size());
        assertEquals(1, productRepository.findById(product.getId()).orElseThrow().getStock());
    }

    @Test
    void invalidStatusTransitionFails() {
        ProductRepository productRepository = new InMemoryProductRepository();
        ProductService productService = new DefaultProductService(productRepository);
        var product = productService.create("seller", "Book", "desc", 20.0, 2, "cat").getData();

        InMemoryCartRepository cartRepository = new InMemoryCartRepository();
        CartService cartService = new DefaultCartService(cartRepository, productRepository);
        cartService.addItem("buyer", product.getId(), 1);

        OrderRepository orderRepository = new InMemoryOrderRepository();
        OrderService orderService = new DefaultOrderService(orderRepository, cartRepository, productRepository);
        Order order = orderService.createFromCart("buyer").getData();

        assertThrows(ValidationException.class, () -> orderService.updateStatus(order.getId(), OrderStatus.COMPLETED));
    }
}
