package c2c.payment;

import c2c.bank.FakeBankService;
import c2c.bank.InMemoryTransactionLogRepository;
import c2c.bank.TransactionLockManager;
import c2c.cart.DefaultCartService;
import c2c.cart.InMemoryCartRepository;
import c2c.common.exceptions.NotFoundException;
import c2c.common.exceptions.ValidationException;
import c2c.bank.BankService;
import c2c.order.DefaultOrderService;
import c2c.order.InMemoryOrderRepository;
import c2c.order.Order;
import c2c.order.OrderRepository;
import c2c.order.OrderStatus;
import c2c.bank.TransactionStatus;
import c2c.product.DefaultProductService;
import c2c.product.InMemoryProductRepository;
import c2c.product.ProductRepository;
import c2c.product.ProductService;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PaymentService 測試案例 (13 個測試)
 * 測試方法: 等價分割、路徑測試、併發測試
 */
class PaymentServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private ProductRepository productRepository;
    private ProductService productService;
    private InMemoryCartRepository cartRepository;
    private DefaultCartService cartService;
    private OrderRepository orderRepository;
    private InMemoryTransactionLogRepository logRepo;
    private PaymentRepository paymentRepository;
    private TransactionLockManager lockManager;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        productService = new DefaultProductService(productRepository);
        cartRepository = new InMemoryCartRepository();
        cartService = new DefaultCartService(cartRepository, productRepository);
        orderRepository = new InMemoryOrderRepository();
        logRepo = new InMemoryTransactionLogRepository();
        paymentRepository = new InMemoryPaymentRepository();
        lockManager = new TransactionLockManager();
    }

    //初始設置訂單
    private Order setupOrder(String buyerId, double price, int quantity) {
        var product = productService.create("seller", "Phone", "desc", price, quantity, "cat").getData();
        cartService.addItem(buyerId, product.getId(), 1);
        var orderService = new DefaultOrderService(orderRepository, cartRepository, productRepository);
        return orderService.createFromCart(buyerId).getData();
    }

    @Test
    @DisplayName("PS-P-001: 等價分割-有效類 - 支付成功 DEBIT方式")
    void PS001_testPaySuccessWithDebitMethod() {
        Order order = setupOrder("buyer", 100.0, 2);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-DEBIT", 500);
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        Payment payment = paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-DEBIT").getData();

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(PaymentMethod.DEBIT, payment.getMethod());
        assertEquals(OrderStatus.PAID, orderRepository.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(1, logRepo.findAll().size());
        assertEquals(TransactionStatus.DEBITED, logRepo.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("PS-P-002: 等價分割-有效類 - 支付成功 CREDIT方式")
    void PS002_testPaySuccessWithCreditMethod() {
        Order order = setupOrder("buyer", 100.0, 2);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-CREDIT", 500);
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        Payment payment = paymentService.pay(order.getId(), PaymentMethod.CREDIT, "buyer", "CARD-CREDIT").getData();

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(PaymentMethod.CREDIT, payment.getMethod());
        assertEquals(OrderStatus.PAID, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("PS-P-003: 等價分割-無效類 - 支付失敗 訂單不存在")
    void PS003_testPayFailsWhenOrderNotFound() {
        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-OK", 500);
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> paymentService.pay("INVALID-ORDER-ID", PaymentMethod.DEBIT, "buyer", "CARD-OK"));

        assertTrue(exception.getMessage().contains("order not found"));
    }

    @Test
    @DisplayName("PS-P-004: 等價分割-無效類 - 支付失敗 訂單狀態非PENDING")
    void PS004_testPayFailsWhenOrderStatusNotPending() {
        Order order = setupOrder("buyer", 100.0, 2);

        // 先支付一次，讓訂單變成 PAID 狀態
        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-OK", 1000);
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-OK");

        // 再次嘗試支付已支付的訂單
        ValidationException exception = assertThrows(ValidationException.class,
                () -> paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-OK"));

        assertTrue(exception.getMessage().contains("order not payable"));
    }

    @Test
    @DisplayName("PS-P-005: 併發測試 - 支付失敗 鎖定超時")
    void PS005_testPayFailsWhenLockTimeout() throws InterruptedException {
        Order order = setupOrder("buyer", 100.0, 2);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-LOCK", 500);
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 50);

        CountDownLatch locked = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            if (lockManager.tryLock("buyer::CARD-LOCK", 1000)) {
                locked.countDown();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                } finally {
                    lockManager.unlock("buyer::CARD-LOCK");
                }
            }
        });
        t.start();
        assertTrue(locked.await(500, TimeUnit.MILLISECONDS));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-LOCK"));

        assertTrue(exception.getMessage().contains("payment lock timeout"));
        t.join();
    }

    @Test
    @DisplayName("PS-P-006: 等價分割-無效類 - 支付失敗 餘額不足")
    void PS006_testPayFailsOnInsufficientBalance() {
        Order order = setupOrder("buyer", 100.0, 2);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-NO-BALANCE", 10);
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-NO-BALANCE"));

        assertTrue(exception.getMessage().contains("insufficient balance"));
        assertEquals(1, logRepo.findAll().size());
        assertEquals(TransactionStatus.FAILED, logRepo.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("PS-P-007: 等價分割-無效類 - 支付失敗 扣款失敗")
    void PS007_testPayFailsWhenBankDebitFails() {
        Order order = setupOrder("buyer", 100.0, 2);

        BankService bankService = mock(BankService.class);
        when(bankService.checkBalance(anyString(), anyString(), anyDouble())).thenReturn(true);
        when(bankService.debit(anyString(), anyString(), anyDouble())).thenReturn(false);

        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-MOCK"));

        assertTrue(exception.getMessage().contains("debit failed"));
        assertEquals(1, logRepo.findAll().size());
        assertEquals(TransactionStatus.FAILED, logRepo.findAll().get(0).getStatus());
    }

    @Test
    @DisplayName("PS-P-008: 路徑測試 - 支付成功後交易記錄正確")
    void PS008_testTransactionLogRecordedOnSuccess() {
        Order order = setupOrder("buyer", 100.0, 2);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-SUCCESS", 500);
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-SUCCESS");

        assertEquals(1, logRepo.findAll().size());
        var transaction = logRepo.findAll().get(0);
        assertEquals(TransactionStatus.DEBITED, transaction.getStatus());
        assertEquals("success", transaction.getMessage());
        assertEquals("buyer::CARD-SUCCESS", transaction.getLockKey());
        assertEquals(100.0, transaction.getAmount());
    }

    @Test
    @DisplayName("PS-P-009: 路徑測試 - 餘額不足時交易記錄正確")
    void PS009_testTransactionLogRecordedOnInsufficientBalance() {
        Order order = setupOrder("buyer", 100.0, 2);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-POOR", 50);
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        assertThrows(ValidationException.class,
                () -> paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-POOR"));

        assertEquals(1, logRepo.findAll().size());
        var transaction = logRepo.findAll().get(0);
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
        assertEquals("insufficient balance", transaction.getMessage());
    }

    @Test
    @DisplayName("PS-P-010: 路徑測試 - 扣款失敗時交易記錄正確")
    void PS010_testTransactionLogRecordedOnDebitFailure() {
        Order order = setupOrder("buyer", 100.0, 2);

        BankService bankService = mock(BankService.class);
        when(bankService.checkBalance(anyString(), anyString(), anyDouble())).thenReturn(true);
        when(bankService.debit(anyString(), anyString(), anyDouble())).thenReturn(false);

        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        assertThrows(ValidationException.class,
                () -> paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-FAIL"));

        assertEquals(1, logRepo.findAll().size());
        var transaction = logRepo.findAll().get(0);
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
        assertEquals("debit failed", transaction.getMessage());
    }

    @Test
    @DisplayName("PS-P-011: 路徑測試 - 支付成功後訂單paymentId正確設置")
    void PS011_testOrderPaymentIdSetCorrectly() {
        Order order = setupOrder("buyer", 100.0, 2);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-VALID", 500);
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        Payment payment = paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-VALID").getData();

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(payment.getId(), updatedOrder.getPaymentId());
        assertNotNull(updatedOrder.getPaymentId());
    }

    @Test
    @DisplayName("PS-P-012: 併發測試 - 兩個線程同時支付不同卡號成功")
    void PS012_testConcurrentPaymentsWithDifferentCardsSucceed() throws InterruptedException {
        Order order1 = setupOrder("buyer1", 100.0, 5);
        Order order2 = setupOrder("buyer2", 150.0, 5);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-1", 500);
        bankService.seedBalance("CARD-2", 500);
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try {
                paymentService.pay(order1.getId(), PaymentMethod.DEBIT, "buyer1", "CARD-1");
                successCount.incrementAndGet();
            } catch (Exception e) {
                // ignore
            } finally {
                latch.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                paymentService.pay(order2.getId(), PaymentMethod.DEBIT, "buyer2", "CARD-2");
                successCount.incrementAndGet();
            } catch (Exception e) {
                // ignore
            } finally {
                latch.countDown();
            }
        });

        t1.start();
        t2.start();
        assertTrue(latch.await(2, TimeUnit.SECONDS));

        assertEquals(2, successCount.get(), "Both payments should succeed with different lock keys");
        assertEquals(2, paymentRepository.findAll().size());
        assertEquals(2, logRepo.findAll().stream().filter(t -> t.getStatus() == TransactionStatus.DEBITED).count());
    }

    @Test
    @DisplayName("PS-P-013: 併發測試 - 鎖定後finally確保釋放")
    void PS013_testLockReleasedInFinallyBlockOnException() {
        Order order = setupOrder("buyer", 100.0, 2);

        BankService bankService = mock(BankService.class);
        when(bankService.checkBalance(anyString(), anyString(), anyDouble())).thenReturn(true);
        doThrow(new RuntimeException("Simulated bank error")).when(bankService).debit(anyString(), anyString(), anyDouble());

        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        assertThrows(RuntimeException.class,
                () -> paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-EXCEPTION"));

        // 驗證鎖已被釋放：嘗試再次獲取鎖應該成功
        boolean canLock = lockManager.tryLock("buyer::CARD-EXCEPTION", 100);
        assertTrue(canLock, "Lock should be released after exception");
        lockManager.unlock("buyer::CARD-EXCEPTION");
    }
}
