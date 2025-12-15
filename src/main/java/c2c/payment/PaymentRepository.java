package c2c.payment;

import java.util.Optional;
import java.util.List;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findByOrderId(String orderId);

    List<Payment> findAll();
}
