package c2c.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BankService (FakeBankService) 測試案例 (6 個測試)
 * 測試方法: 等價分割、邊界值分析
 */
class FakeBankServiceTest {

    private FakeBankService bankService;

    @BeforeEach
    void setUp() {
        bankService = new FakeBankService();
    }

    /**
     * BS-C-001: 檢查餘額成功 - 餘額充足
     * 測試方法: 等價分割-有效類
     * 輸入: cardNo: valid(balance=100), amount: 50
     * 預期結果: checkBalance返回true
     * 分支覆蓋: balance>=amount 分支
     */
    @Test
    void BS_C001_testCheckBalanceSuccess_SufficientBalance() {
        // Arrange
        String payerId = "payer1";
        String cardNumber = "1234-5678-9012-3456";
        bankService.seedBalance(cardNumber, 100.0);
        double amount = 50.0;

        // Act
        boolean result = bankService.checkBalance(payerId, cardNumber, amount);

        // Assert
        assertTrue(result, "餘額充足時應該返回true");
    }

    /**
     * BS-C-002: 檢查餘額失敗 - 餘額不足
     * 測試方法: 等價分割-無效類
     * 輸入: cardNo: valid(balance=100), amount: 150
     * 預期結果: checkBalance返回false
     * 分支覆蓋: balance<amount 分支
     */
    @Test
    void BS_C002_testCheckBalanceFails_InsufficientBalance() {
        // Arrange
        String payerId = "payer1";
        String cardNumber = "1234-5678-9012-3456";
        bankService.seedBalance(cardNumber, 100.0);
        double amount = 150.0;

        // Act
        boolean result = bankService.checkBalance(payerId, cardNumber, amount);

        // Assert
        assertFalse(result, "餘額不足時應該返回false");
    }

    /**
     * BS-C-003: 檢查餘額失敗 - 卡號不存在
     * 測試方法: 等價分割-無效類
     * 輸入: cardNo: invalid, amount: 50
     * 預期結果: checkBalance返回false (default 0.0)
     * 分支覆蓋: getOrDefault 分支
     */
    @Test
    void BS_C003_testCheckBalanceFails_CardNotExists() {
        // Arrange
        String payerId = "payer1";
        String cardNumber = "9999-9999-9999-9999"; // 不存在的卡號
        double amount = 50.0;

        // Act
        boolean result = bankService.checkBalance(payerId, cardNumber, amount);

        // Assert
        assertFalse(result, "卡號不存在時應該返回false (default balance=0.0)");
    }

    /**
     * BS-D-001: 扣款成功 - 餘額充足
     * 測試方法: 等價分割-有效類
     * 輸入: cardNo: valid(balance=100), amount: 50
     * 預期結果: debit返回true, balance減少到50
     * 分支覆蓋: computeIfPresent成功, balance>=amount
     */
    @Test
    void BS_D001_testDebitSuccess_SufficientBalance() {
        // Arrange
        String payerId = "payer1";
        String cardNumber = "1234-5678-9012-3456";
        bankService.seedBalance(cardNumber, 100.0);
        double amount = 50.0;

        // Act
        boolean result = bankService.debit(payerId, cardNumber, amount);

        // Assert
        assertTrue(result, "餘額充足時扣款應該成功");

        // 驗證餘額是否正確扣減
        boolean balanceCheck = bankService.checkBalance(payerId, cardNumber, 50.0);
        assertTrue(balanceCheck, "扣款後剩餘餘額應該為50.0");

        boolean exactBalanceCheck = bankService.checkBalance(payerId, cardNumber, 50.01);
        assertFalse(exactBalanceCheck, "扣款後剩餘餘額應該恰好為50.0");
    }

    /**
     * BS-D-002: 扣款失敗 - 餘額不足
     * 測試方法: 等價分割-無效類
     * 輸入: cardNo: valid(balance=100), amount: 150
     * 預期結果: debit返回false, balance不變
     * 分支覆蓋: balance<amount 分支
     */
    @Test
    void BS_D002_testDebitFails_InsufficientBalance() {
        // Arrange
        String payerId = "payer1";
        String cardNumber = "1234-5678-9012-3456";
        bankService.seedBalance(cardNumber, 100.0);
        double amount = 150.0;

        // Act
        boolean result = bankService.debit(payerId, cardNumber, amount);

        // Assert
        assertFalse(result, "餘額不足時扣款應該失敗");

        // 驗證餘額未變動
        boolean balanceCheck = bankService.checkBalance(payerId, cardNumber, 100.0);
        assertTrue(balanceCheck, "扣款失敗後餘額應該保持不變");
    }

    /**
     * BS-D-003: 扣款失敗 - 卡號不存在
     * 測試方法: 等價分割-無效類
     * 輸入: cardNo: invalid, amount: 50
     * 預期結果: debit返回false
     * 分支覆蓋: computeIfPresent失敗 (key不存在)
     */
    @Test
    void BS_D003_testDebitFails_CardNotExists() {
        // Arrange
        String payerId = "payer1";
        String cardNumber = "9999-9999-9999-9999"; // 不存在的卡號
        double amount = 50.0;

        // Act
        boolean result = bankService.debit(payerId, cardNumber, amount);

        // Assert
        assertFalse(result, "卡號不存在時扣款應該失敗");
    }

    /**
     * 額外測試: 邊界值測試 - 扣款金額等於餘額
     * 驗證邊界條件 balance == amount
     */
    @Test
    void testDebitSuccess_ExactBalance() {
        // Arrange
        String payerId = "payer1";
        String cardNumber = "1234-5678-9012-3456";
        bankService.seedBalance(cardNumber, 100.0);
        double amount = 100.0;

        // Act
        boolean result = bankService.debit(payerId, cardNumber, amount);

        // Assert
        assertTrue(result, "扣款金額等於餘額時應該成功");

        // 驗證餘額變為0
        boolean zeroBalanceCheck = bankService.checkBalance(payerId, cardNumber, 0.01);
        assertFalse(zeroBalanceCheck, "扣款後餘額應該為0");
    }

    /**
     * 額外測試: 邊界值測試 - 扣款金額略大於餘額
     * 驗證邊界條件 amount > balance (微小差異)
     */
    @Test
    void testDebitFails_SlightlyInsufficientBalance() {
        // Arrange
        String payerId = "payer1";
        String cardNumber = "1234-5678-9012-3456";
        bankService.seedBalance(cardNumber, 100.0);
        double amount = 100.01;

        // Act
        boolean result = bankService.debit(payerId, cardNumber, amount);

        // Assert
        assertFalse(result, "扣款金額略大於餘額時應該失敗");

        // 驗證餘額未變動
        boolean balanceCheck = bankService.checkBalance(payerId, cardNumber, 100.0);
        assertTrue(balanceCheck, "扣款失敗後餘額應該保持100.0");
    }

    /**
     * 額外測試: 併發安全性 - 多次扣款
     * 驗證 ConcurrentHashMap 的執行緒安全性
     */
    @Test
    void testConcurrentDebits() throws InterruptedException {
        // Arrange
        String payerId = "payer1";
        String cardNumber = "1234-5678-9012-3456";
        bankService.seedBalance(cardNumber, 100.0);

        // Act - 連續扣款
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                bankService.debit(payerId, cardNumber, 10.0);
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                bankService.debit(payerId, cardNumber, 10.0);
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Assert - 總共扣款100，餘額應為0
        boolean hasNoBalance = !bankService.checkBalance(payerId, cardNumber, 0.01);
        assertTrue(hasNoBalance, "併發扣款後餘額應該為0");
    }
}
