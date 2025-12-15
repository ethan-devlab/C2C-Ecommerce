package c2c.payment;

import c2c.bank.BankService;
import c2c.bank.Transaction;
import c2c.bank.TransactionLockManager;
import c2c.bank.TransactionLogRepository;
import c2c.bank.TransactionStatus;
import c2c.common.IdGenerator;
import c2c.common.Result;
import c2c.common.exceptions.NotFoundException;
import c2c.common.exceptions.ValidationException;
import c2c.order.Order;
import c2c.order.OrderRepository;
import c2c.order.OrderStatus;

import java.time.Instant;

public class DefaultPaymentService implements PaymentService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final BankService bankService;
    private final TransactionLockManager lockManager;
    private final TransactionLogRepository transactionLogRepository;
    private final long lockTimeoutMillis;

    public DefaultPaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository,
            BankService bankService, TransactionLockManager lockManager,
            TransactionLogRepository transactionLogRepository, long lockTimeoutMillis) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.bankService = bankService;
        this.lockManager = lockManager;
        this.transactionLogRepository = transactionLogRepository;
        this.lockTimeoutMillis = lockTimeoutMillis;
    }

    @Override
    public Result<Payment> pay(String orderId, PaymentMethod method, String payerId, String cardNumber) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ValidationException("order not payable");
        }
        String lockKey = payerId + "::" + cardNumber;
        boolean locked = lockManager.tryLock(lockKey, lockTimeoutMillis);
        if (!locked) {
            throw new ValidationException("payment lock timeout");
        }

        Transaction transaction = new Transaction(IdGenerator.nextNumeric(), payerId, order.getAmount(), method,
                lockKey, TransactionStatus.LOCKED, Instant.now(), null, null);
        try {
            if (!bankService.checkBalance(payerId, cardNumber, order.getAmount())) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setMessage("insufficient balance");
                transactionLogRepository.save(transaction);
                throw new ValidationException("insufficient balance");
            }
            boolean debited = bankService.debit(payerId, cardNumber, order.getAmount());
            if (!debited) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setMessage("debit failed");
                transactionLogRepository.save(transaction);
                throw new ValidationException("debit failed");
            }
            transaction.setStatus(TransactionStatus.DEBITED);
            transaction.setMessage("success");
            transactionLogRepository.save(transaction);

            Payment payment = new Payment(IdGenerator.nextNumeric(), orderId, method, order.getAmount(),
                    PaymentStatus.SUCCESS, transaction.getId(), Instant.now());
            paymentRepository.save(payment);
            order.setStatus(OrderStatus.PAID);
            order.setPaymentId(payment.getId());
            orderRepository.save(order);
            return Result.ok(payment);
        } finally {
            lockManager.unlock(lockKey);
        }
    }
}
