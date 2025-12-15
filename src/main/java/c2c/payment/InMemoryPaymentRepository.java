package c2c.payment;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

public class InMemoryPaymentRepository implements PaymentRepository {
    private final Map<String, Payment> storage = new ConcurrentHashMap<>();
    private final Map<String, String> orderIndex = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        storage.put(payment.getId(), payment);
        orderIndex.put(payment.getOrderId(), payment.getId());
        return payment;
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        String id = orderIndex.get(orderId);
        return id == null ? Optional.empty() : Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Payment> findAll() {
        return new ArrayList<>(storage.values());
    }
}
