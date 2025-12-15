package c2c.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepository implements OrderRepository {
    private final Map<String, Order> storage = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        storage.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Order> listByBuyer(String buyerId) {
        List<Order> result = new ArrayList<>();
        for (Order order : storage.values()) {
            if (order.getBuyerId().equals(buyerId)) {
                result.add(order);
            }
        }
        return result;
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(storage.values());
    }
}
