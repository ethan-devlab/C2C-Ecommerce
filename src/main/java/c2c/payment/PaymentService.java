package c2c.payment;

import c2c.common.Result;

public interface PaymentService {
    Result<Payment> pay(String orderId, PaymentMethod method, String payerId, String cardNumber);
}
