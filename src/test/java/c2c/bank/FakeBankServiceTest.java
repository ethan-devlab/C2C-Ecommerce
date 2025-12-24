package c2c.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @Test
    @DisplayName("BS-C-001: 檢查餘額成功 - 餘額充足")
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

    @Test
    @DisplayName("BS-C-002: 檢查餘額失敗 - 餘額不足")
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

    @Test
    @DisplayName("BS-C-003: 檢查餘額失敗 - 卡號不存在")
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

    @Test
    @DisplayName("BS-D-001: 扣款成功 - 餘額充足")
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

    @Test
    @DisplayName("BS-D-002: 扣款失敗 - 餘額不足")
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

    @Test
    @DisplayName("BS-D-003: 扣款失敗 - 卡號不存在")
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
}
