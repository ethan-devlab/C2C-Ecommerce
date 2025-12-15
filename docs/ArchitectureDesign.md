# C2C 平台架構設計 (Architecture Design Document)

## 1. 架構概觀
- 分層：`cli/ui` → `application` (service + use case) → `domain` (entities + policy) → `infrastructure` (persistence in-memory, logging, locking, id generation, serialization)。
- 主要目標：模組化、可測試、可替換資料儲存；以介面隔離方便日後接 DB/GUI。

## 2. 模組與封裝建議 (package)
- `c2c.common`：例外階層、Result/Response 封裝、序列化工具、Id 產生器。
- `c2c.user`：User, UserRepository, UserService, PasswordHasher。
- `c2c.product`：Product, ProductRepository, ProductService, SearchSpec。
- `c2c.cart`：CartItem, CartService, CartRepository。
- `c2c.order`：Order, OrderItem, OrderRepository, OrderService, OrderStatusMachine。
- `c2c.payment`：Payment, PaymentService, PaymentMethod (CREDIT, DEBIT)。
- `c2c.bank`：BankService, Transaction, TransactionLockManager, TransactionLogRepository。
- `c2c.review`：Review, ReviewRepository, ReviewService。
- `c2c.cli`：命令解析、輸入驗證、格式化輸出。
- `c2c.logging`：Log4j2 設定、審計工具。

## 3. 元件關係 (文字化)
- `CLI` 呼叫各 `Service`；Service 依賴對應 `Repository` 與交叉服務（例如 `OrderService` 依賴 `ProductService`, `CartService`, `PaymentService`）。
- `PaymentService` 依賴 `BankService` 處理扣款；`BankService` 使用 `TransactionLockManager` 以 lockKey（如 userId+cardNo）執行互斥。
- `OrderService` 更新 `Product` 庫存（透過 `ProductRepository`），並與 `PaymentService` 互動；失敗則回滾狀態。
- `ReviewService` 依賴 `OrderService`/`OrderRepository` 驗證訂單已完成。

## 4. 高層時序（下單與支付）
1) Buyer 在 CLI 呼叫 `OrderService.createOrder(buyerId, cartItems)` → 建立訂單 PENDING，扣減預留庫存。
2) `PaymentService.pay(orderId, method, cardInfo)` → `TransactionLockManager.lock(key)` → `BankService.debit(...)` 檢查餘額、扣款。
3) 成功：`PaymentService` 標記 Payment PAID，`OrderService` 狀態改 PAID，釋放鎖，記錄交易 log。
4) 失敗：釋放鎖，`OrderService` 標記 CANCELED/FAILED，恢復庫存，回傳錯誤訊息。

## 5. 併發與交易鎖
- `TransactionLockManager`：基於 `ConcurrentHashMap<LockKey, ReentrantLock>`；支援 tryLock + timeout；確保同一使用者或同一卡號的交易互斥。
- 失敗或超時回傳適當錯誤碼；所有鎖釋放放在 finally。

## 6. 資料持久化策略（無外部 DB）
- In-memory repository：`Map<Id, Entity>`；啟動時可選擇從 JSON 匯入；關閉時序列化為檔案（可選）。
- 為測試可注入替代實作（Fake/Stub）。

## 7. 日誌與監控
- Log4j2 配置：Console appender；pattern 包含時間/等級/模組/追蹤 ID。
- 審計事件：登入、註冊、交易、訂單狀態變更、評價新增；例外須記錄 stack trace。

## 8. 安全性
- 密碼雜湊介面 `PasswordHasher`（預設 BCrypt）；輸入驗證集中於 CLI 層；避免在日誌中寫入密碼與卡號。

## 9. 可測試性設計
- 所有 Service 以介面暴露；Repository/BankService 可用 Mockito mock；TransactionLockManager 提供 hook 以注入測試鎖。
- 狀態機封裝：OrderStatusMachine 單獨測試合法流轉；避免分散在多處。

## 10. 未來擴充
- GUI/Swing/JavaFX 僅需替換 `cli` 層；持久化可換成 DB 實作；Lock 可替換為分散式鎖；支付可接第三方。
