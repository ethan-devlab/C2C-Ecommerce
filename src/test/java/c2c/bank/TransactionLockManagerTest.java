package c2c.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
/**
 * TransactionLockManager 測試案例 (5 個測試)
 * 測試方法: 併發測試、路徑測試
 */
class TransactionLockManagerTest {

    private TransactionLockManager lockManager;

    @BeforeEach
    void setUp() {
        lockManager = new TransactionLockManager();
    }

    @Test
    @DisplayName("TL-001: 獲取鎖成功")
    void TL001_testTryLockSuccess() {
        // Arrange
        String lockKey = "test";
        long timeout = 1000;

        // Act
        boolean result = lockManager.tryLock(lockKey, timeout);

        // Assert
        assertTrue(result, "應該成功獲取鎖");

        // Cleanup
        lockManager.unlock(lockKey);
    }


    @Test
    @DisplayName("TL-002: 已鎖定時獲取鎖失敗")
    void TL002_testTryLockFailsWhenAlreadyLocked() throws InterruptedException {
        // Arrange
        String lockKey = "test";
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicBoolean thread1Locked = new AtomicBoolean(false);
        AtomicBoolean thread2Locked = new AtomicBoolean(false);

        // 執行緒1先獲取鎖
        Thread thread1 = new Thread(() -> {
            thread1Locked.set(lockManager.tryLock(lockKey, 1000));
            latch1.countDown(); // 通知執行緒1已獲取鎖
            try {
                latch2.await(); // 等待執行緒2嘗試獲取鎖後再釋放
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            lockManager.unlock(lockKey);
        });

        thread1.start();
        latch1.await(); // 等待執行緒1獲取鎖

        // Act - 執行緒2嘗試獲取同一個鎖 (應該失敗)
        thread2Locked.set(lockManager.tryLock(lockKey, 100)); // 短超時時間
        latch2.countDown(); // 通知執行緒1可以釋放鎖

        thread1.join();

        // Assert
        assertTrue(thread1Locked.get(), "執行緒1應該成功獲取鎖");
        assertFalse(thread2Locked.get(), "執行緒2應該無法獲取已被持有的鎖");
    }

    @Test
    @DisplayName("TL-003: 釋放鎖成功")
    void TL003_testUnlockSuccess() {
        // Arrange
        String lockKey = "test";
        boolean locked = lockManager.tryLock(lockKey, 1000);
        assertTrue(locked, "前置條件: 應該成功獲取鎖");

        // Act
        lockManager.unlock(lockKey);

        // Assert - 釋放後其他人應該能獲取鎖
        boolean canLockAgain = lockManager.tryLock(lockKey, 1000);
        assertTrue(canLockAgain, "釋放鎖後應該能再次獲取鎖");

        // Cleanup
        lockManager.unlock(lockKey);
    }

    @Test
    @DisplayName("TL-004: 釋放未持有的鎖不拋異常")
    void TL004_testUnlockNotHeldLockDoesNotThrow() {
        // Act & Assert - 釋放不存在的鎖 (lock==null 分支)
        assertDoesNotThrow(() -> lockManager.unlock("nonexistent"),
                "釋放不存在的鎖不應拋出異常");

        // Act & Assert - 釋放未持有的鎖 (!isHeldByCurrentThread() 分支)
        String lockKey = "test";
        lockManager.tryLock(lockKey, 1000);
        lockManager.unlock(lockKey);

        // 再次釋放同一個鎖 (已經釋放過，不再持有)
        assertDoesNotThrow(() -> lockManager.unlock(lockKey),
                "釋放已釋放的鎖不應拋出異常");
    }

    @Test
    @DisplayName("TL-005: 執行緒中斷時tryLock返回false")
    void TL005_testTryLockReturnsFalseWhenThreadInterrupted() throws InterruptedException {
        // Arrange
        String lockKey = "test";
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicBoolean thread1Locked = new AtomicBoolean(false);
        AtomicBoolean thread2Locked = new AtomicBoolean(false);
        AtomicBoolean thread2Interrupted = new AtomicBoolean(false);

        // 執行緒1先獲取鎖並持有
        Thread thread1 = new Thread(() -> {
            thread1Locked.set(lockManager.tryLock(lockKey, 5000));
            latch1.countDown(); // 通知已獲取鎖
            try {
                latch2.await(); // 等待執行緒2被中斷後再釋放
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            lockManager.unlock(lockKey);
        });

        // 執行緒2嘗試獲取同一個鎖，並在等待時被中斷
        Thread thread2 = new Thread(() -> {
            try {
                latch1.await(); // 等待執行緒1獲取鎖
                thread2Locked.set(lockManager.tryLock(lockKey, 5000)); // 長超時，會被中斷
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread2Interrupted.set(Thread.currentThread().isInterrupted());
        });

        // Act
        thread1.start();
        thread2.start();

        // 等待執行緒2開始嘗試獲取鎖
        Thread.sleep(100);

        // 中斷執行緒2
        thread2.interrupt();
        latch2.countDown(); // 通知執行緒1可以釋放鎖

        thread1.join();
        thread2.join();

        // Assert
        assertTrue(thread1Locked.get(), "執行緒1應該成功獲取鎖");
        assertFalse(thread2Locked.get(), "執行緒2在被中斷時應該返回false");
        assertTrue(thread2Interrupted.get(), "執行緒2的中斷標誌應該被設置");
    }
}
