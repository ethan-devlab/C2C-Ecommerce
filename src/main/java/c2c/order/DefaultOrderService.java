package c2c.order;

import c2c.cart.CartItem;
import c2c.cart.CartRepository;
import c2c.common.IdGenerator;
import c2c.common.Result;
import c2c.common.exceptions.NotFoundException;
import c2c.common.exceptions.ValidationException;
import c2c.product.Product;
import c2c.product.ProductRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DefaultOrderService implements OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public DefaultOrderService(OrderRepository orderRepository, CartRepository cartRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Result<Order> createFromCart(String buyerId) {
        List<CartItem> cart = cartRepository.getCart(buyerId);
        if (cart.isEmpty()) {
            throw new ValidationException("cart is empty");
        }
        List<OrderItem> items = new ArrayList<>();
        double amount = 0;
        for (CartItem item : cart) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("product not found"));
            if (product.getStock() < item.getQuantity()) {
                throw new ValidationException("insufficient stock for product " + product.getId());
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
            items.add(new OrderItem(product.getId(), product.getTitle(), product.getPrice(), item.getQuantity(),
                    product.getSellerId()));
            amount += product.getPrice() * item.getQuantity();
        }
        Order order = new Order(IdGenerator.nextNumeric(), buyerId, items, amount, OrderStatus.PENDING, null,
                Instant.now(), Instant.now());
        orderRepository.save(order);
        cartRepository.clear(buyerId);
        return Result.ok(order);
    }

    @Override
    public Result<Order> get(String orderId) {
        return Result.ok(orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("order not found")));
    }

    @Override
    public Result<List<Order>> listByBuyer(String buyerId) {
        return Result.ok(orderRepository.listByBuyer(buyerId));
    }

    @Override
    public Result<Order> updateStatus(String orderId, OrderStatus toStatus) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("order not found"));
        if (!OrderStatusMachine.canTransit(order.getStatus(), toStatus)) {
            throw new ValidationException("invalid status transition");
        }
        order.setStatus(toStatus);
        orderRepository.save(order);
        return Result.ok(order);
    }
}
