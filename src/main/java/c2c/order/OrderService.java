package c2c.order;

import c2c.common.Result;

import java.util.List;

public interface OrderService {
    Result<Order> createFromCart(String buyerId);

    Result<Order> get(String orderId);

    Result<List<Order>> listByBuyer(String buyerId);

    Result<Order> updateStatus(String orderId, OrderStatus toStatus);
}
