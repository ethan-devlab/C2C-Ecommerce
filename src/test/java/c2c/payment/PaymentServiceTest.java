package c2c.payment;

import c2c.bank.FakeBankService;
import c2c.bank.InMemoryTransactionLogRepository;
import c2c.bank.TransactionLockManager;
import c2c.cart.DefaultCartService;
import c2c.cart.InMemoryCartRepository;
import c2c.common.exceptions.ValidationException;
import c2c.bank.BankService;
import c2c.order.DefaultOrderService;
import c2c.order.InMemoryOrderRepository;
import c2c.order.Order;
import c2c.order.OrderRepository;
import c2c.bank.TransactionStatus;
import c2c.product.DefaultProductService;
import c2c.product.InMemoryProductRepository;
import c2c.product.ProductRepository;
import c2c.product.ProductService;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private Order setupOrder(ProductRepository productRepository, ProductService productService,
            InMemoryCartRepository cartRepository, DefaultCartService cartService, OrderRepository orderRepository) {
        var product = productService.create("seller", "Phone", "desc", 100.0, 2, "cat").getData();
        cartService.addItem("buyer", product.getId(), 1);
        var orderService = new DefaultOrderService(orderRepository, cartRepository, productRepository);
        return orderService.createFromCart("buyer").getData();
    }

    @Test
    void paySuccessUpdatesOrder() {
        ProductRepository productRepository = new InMemoryProductRepository();
        ProductService productService = new DefaultProductService(productRepository);
        InMemoryCartRepository cartRepository = new InMemoryCartRepository();
        DefaultCartService cartService = new DefaultCartService(cartRepository, productRepository);
        OrderRepository orderRepository = new InMemoryOrderRepository();
        Order order = setupOrder(productRepository, productService, cartRepository, cartService, orderRepository);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-OK", 500);
        TransactionLockManager lockManager = new TransactionLockManager();
        var logRepo = new InMemoryTransactionLogRepository();
        PaymentRepository paymentRepository = new InMemoryPaymentRepository();
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        Payment payment = paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-OK").getData();
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(1, logRepo.findAll().size());
        assertEquals("buyer::CARD-OK", logRepo.findAll().get(0).getLockKey());
        assertEquals(TransactionStatus.DEBITED, logRepo.findAll().get(0).getStatus());
        assertEquals(c2c.order.OrderStatus.PAID, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void payFailsWhenLocked() throws InterruptedException {
        ProductRepository productRepository = new InMemoryProductRepository();
        ProductService productService = new DefaultProductService(productRepository);
        InMemoryCartRepository cartRepository = new InMemoryCartRepository();
        DefaultCartService cartService = new DefaultCartService(cartRepository, productRepository);
        OrderRepository orderRepository = new InMemoryOrderRepository();
        Order order = setupOrder(productRepository, productService, cartRepository, cartService, orderRepository);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-LOCK", 500);
        TransactionLockManager lockManager = new TransactionLockManager();
        var logRepo = new InMemoryTransactionLogRepository();
        PaymentRepository paymentRepository = new InMemoryPaymentRepository();
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

        assertThrows(ValidationException.class,
                () -> paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-LOCK"));
    }

    @Test
    void payFailsWhenBankDebitFails() {
        ProductRepository productRepository = new InMemoryProductRepository();
        ProductService productService = new DefaultProductService(productRepository);
        InMemoryCartRepository cartRepository = new InMemoryCartRepository();
        DefaultCartService cartService = new DefaultCartService(cartRepository, productRepository);
        OrderRepository orderRepository = new InMemoryOrderRepository();
        Order order = setupOrder(productRepository, productService, cartRepository, cartService, orderRepository);

        BankService bankService = mock(BankService.class);
        when(bankService.checkBalance(anyString(), anyString(), anyDouble())).thenReturn(true);
        when(bankService.debit(anyString(), anyString(), anyDouble())).thenReturn(false);

        TransactionLockManager lockManager = new TransactionLockManager();
        var logRepo = new InMemoryTransactionLogRepository();
        PaymentRepository paymentRepository = new InMemoryPaymentRepository();
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        assertThrows(ValidationException.class,
                () -> paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-MOCK"));
        assertEquals(1, logRepo.findAll().size());
        assertEquals(TransactionStatus.FAILED, logRepo.findAll().get(0).getStatus());
    }

    @Test
    void payFailsOnInsufficientBalance() {
        ProductRepository productRepository = new InMemoryProductRepository();
        ProductService productService = new DefaultProductService(productRepository);
        InMemoryCartRepository cartRepository = new InMemoryCartRepository();
        DefaultCartService cartService = new DefaultCartService(cartRepository, productRepository);
        OrderRepository orderRepository = new InMemoryOrderRepository();
        Order order = setupOrder(productRepository, productService, cartRepository, cartService, orderRepository);

        FakeBankService bankService = new FakeBankService();
        bankService.seedBalance("CARD-NO", 10);
        TransactionLockManager lockManager = new TransactionLockManager();
        var logRepo = new InMemoryTransactionLogRepository();
        PaymentRepository paymentRepository = new InMemoryPaymentRepository();
        PaymentService paymentService = new DefaultPaymentService(orderRepository, paymentRepository, bankService,
                lockManager, logRepo, 200);

        assertThrows(ValidationException.class,
                () -> paymentService.pay(order.getId(), PaymentMethod.DEBIT, "buyer", "CARD-NO"));
        assertEquals(1, logRepo.findAll().size());
        assertEquals(TransactionStatus.FAILED, logRepo.findAll().get(0).getStatus());
    }
}
