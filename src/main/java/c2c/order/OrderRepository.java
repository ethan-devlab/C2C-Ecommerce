package c2c.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> listByBuyer(String buyerId);

    List<Order> findAll();
}
